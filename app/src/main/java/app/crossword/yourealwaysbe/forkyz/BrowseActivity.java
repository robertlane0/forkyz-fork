package app.crossword.yourealwaysbe.forkyz;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.IntentCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import app.crossword.yourealwaysbe.forkyz.databinding.BrowseBinding;
import app.crossword.yourealwaysbe.forkyz.databinding.PuzzleListItemBinding;
import app.crossword.yourealwaysbe.forkyz.net.Downloader;
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider;
import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation;
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager;
import app.crossword.yourealwaysbe.forkyz.util.DownloadPickerDialogBuilder;
import app.crossword.yourealwaysbe.forkyz.util.MigrationHelper;
import app.crossword.yourealwaysbe.forkyz.util.files.Accessor;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider;
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle;
import app.crossword.yourealwaysbe.forkyz.util.files.PuzMetaFile;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.forkyz.view.recycler.RemovableRecyclerViewAdapter;
import app.crossword.yourealwaysbe.forkyz.view.recycler.SeparatedRecyclerViewAdapter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.inject.Inject;

@AndroidEntryPoint
public class BrowseActivity extends ForkyzActivity {

    /**
     * Request other browsers close
     *
     * An intent to broadcast from BrowseActivity to tell all other
     * BrowseActivities to close down (avoid multiple instances on same
     * file system)
     *
     * launchMode singleTask is not really what we want here as it
     * prevents the user from returning to a puzzle from the home screen
     * (annoying)
     */
    private static final String BROWSER_CLOSE_ACTION
        = "app.crossword.yourealwaysbe.BROWSER_CLOSE_ACTION";
    /**
     * The task ID of the BrowseActivity requesting the close
     *
     * To avoid closing self
     */
    private static final String BROWSER_CLOSE_TASK_ID
        = "app.crossword.yourealwaysbe.BROWSER_CLOSE_TASK_ID";

    /**
     * So the imports complete dialog knows if there was success or not
     */
    private static final String IMPORT_SUCCESS = "importSuccess";

    // allow import of all docs (parser will take care of detecting if it's a
    // puzzle that's recognised)
    private static final String IMPORT_MIME_TYPE =  "*/*";

    private static final Logger LOGGER
        = Logger.getLogger(BrowseActivity.class.getCanonicalName());

    private static final DateTimeFormatter DATE_FORMAT
        = DateTimeFormatter.ofPattern("EEEE\ndd MMM\nyyyy");

    @Inject
    protected MigrationHelper migrationHelper;

    @Inject
    protected BackgroundDownloadManager backgroundDownloadManager;

    @Inject
    protected FileHandlerProvider fileHandlerProvider;

    @Inject
    protected DownloadersProvider downloadersProvider;

    /**
     * See note for BROWSER_CLOSE_ACTION
     */
    private BroadcastReceiver closeActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BROWSER_CLOSE_ACTION)) {
                int myTaskId = BrowseActivity.this.getTaskId();
                int otherTaskId
                    = intent.getIntExtra(BROWSER_CLOSE_TASK_ID, myTaskId);

                if (myTaskId != otherTaskId) {
                    utils.finishAndRemoveTask(BrowseActivity.this);
                }
            }
        }
    };

    /**
     * When POST_NOTIFICATIONS permission needed
     */
    private ActivityResultLauncher<String> notificationPermissionLauncher
        = registerForActivityResult(new RequestPermission(), isGranted -> {
            if (!isGranted) {
                DialogFragment dialog = new NotificationPermissionDialog();
                dialog.show(
                    getSupportFragmentManager(), "NotificationPermissionDialog"
                );
            }
        });

    /**
     * When WRITE_EXTERNAL_STORAGE permission needed
     */
    private ActivityResultLauncher<String> writeStorageLauncher
        = registerForActivityResult(new RequestPermission(), isGranted -> {
            // do things that would have been abandoned by permission
            // missing
            autoDownloadIfEnabled();
            startLoadPuzzleList();
        });

    private BrowseActivityViewModel model;

    private BrowseBinding binding;
    private SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter>
        currentAdapter = null;
    private Handler handler = new Handler(Looper.getMainLooper());
    private NotificationManagerCompat nm;
    private Set<PuzMetaFile> selected = new HashSet<>();
    private MenuItem viewCrosswordsArchiveMenuItem;
    private Collection<Uri> pendingImports = null;

    // setup in onCreate as utils injection needed
    ActivityResultLauncher<String> getImportURI;

    private ActionMode actionMode;
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.browse_action_bar_menu, menu);

            if (model.getIsViewArchive()) {
                menu.findItem(R.id.browse_action_archive)
                    .setVisible(false);
            } else {
                menu.findItem(R.id.browse_action_unarchive)
                    .setVisible(false);
            }

            for (int i = 0; i < menu.size(); i++) {
                onActionBarWithText(menu.getItem(i));
            }

            setSpeedDialVisibility(View.GONE);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(
            ActionMode actionMode, MenuItem menuItem
        ) {
            int id = menuItem.getItemId();

            Set<PuzMetaFile> toAction = new HashSet<>(selected);

            fileHandlerProvider.get(fileHandler -> {
                if (id == R.id.browse_action_delete) {
                    model.deletePuzzles(toAction);
                } else if (id == R.id.browse_action_archive) {
                    model.movePuzzles(
                        toAction, fileHandler.getArchiveDirectory()
                    );
                } else if (id == R.id.browse_action_unarchive) {
                    model.movePuzzles(
                        toAction, fileHandler.getCrosswordsDirectory()
                    );
                }
            });

            actionMode.finish();

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            clearSelection();
            setSpeedDialVisibility(View.VISIBLE);
            actionMode = null;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // for parity with onKeyUp
        switch (keyCode) {
        case KeyEvent.KEYCODE_ESCAPE:
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_ESCAPE:
            finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browse_menu, menu);

        MenuItem item = menu.findItem(R.id.browse_menu_app_theme);
        if (item != null) {
            nightMode.getCurrentMode(mode -> {
                item.setIcon(getNightModeIcon(mode));
            });
        }

        viewCrosswordsArchiveMenuItem
            = menu.findItem(R.id.browse_menu_archives);

        setViewCrosswordsOrArchiveUI();

        return true;
    }

    private int getNightModeIcon(DayNightMode mode) {
        switch (mode) {
        case DAY: return R.drawable.day_mode;
        case NIGHT: return R.drawable.night_mode;
        case SYSTEM: return R.drawable.system_daynight_mode;
        }
        return R.drawable.day_mode;
    }

    private void setListItemColor(View v, boolean selected){
        v.setSelected(selected);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.browse_menu_filter) {
            showPuzzleFilter();
            return true;
        } else if (id == R.id.browse_menu_app_theme) {
            nightMode.next(newMode -> {
                item.setIcon(getNightModeIcon(newMode));
            });
            return true;
        } else if (id == R.id.browse_menu_settings) {
            Intent settingsIntent = new Intent(this, PreferencesActivity.class);
            this.startActivity(settingsIntent);
            return true;
        } else if (id == R.id.browse_menu_archives) {
            startLoadPuzzleList(!model.getIsViewArchive());
            return true;
        } else if (id == R.id.browse_menu_cleanup) {
            model.cleanUpPuzzles();
            return true;
        } else if (id == R.id.browse_menu_help) {
            Intent helpIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("filescreen.html"),
                this,
                HTMLActivity.class
            );
            this.startActivity(helpIntent);
            return true;
        } else if (id == R.id.browse_menu_sort_source) {
            settings.setBrowseSort(Accessor.SOURCE);
            return true;
        } else if (id == R.id.browse_menu_sort_date_asc) {
            settings.setBrowseSort(Accessor.DATE_ASC);
            return true;
        } else if (id == R.id.browse_menu_sort_date_desc) {
            settings.setBrowseSort(Accessor.DATE_DESC);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ask others to close down
        sendBroadcast(
            new Intent(BROWSER_CLOSE_ACTION).putExtra(
                BROWSER_CLOSE_TASK_ID, getTaskId()
            )
        );

        // listen for others closing us down
        registerReceiver(
            closeActionReceiver, new IntentFilter(BROWSER_CLOSE_ACTION)
        );

        // Bring up to date
        migrationHelper.applyMigrations(this);

        // Now create!

        getImportURI = utils.registerForUriContentsResult(
            this,
            uris -> { onImportURIs(uris); }
        );

        binding = BrowseBinding.inflate(getLayoutInflater());
        this.setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        setupBottomInsets(binding.content);
        setupBottomInsets(binding.speedDialAdd);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        binding.puzzleList.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper helper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.START | ItemTouchHelper.END
            ) {
                private LiveData<Boolean> liveDisableSwipe
                    = settings.liveBrowseDisableSwipe();

                @Override
                public int getSwipeDirs(
                    RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder
                ) {
                    Boolean disableSwipe = liveDisableSwipe.getValue();
                    if (disableSwipe == null)
                        disableSwipe = true;

                    if (!(viewHolder instanceof FileViewHolder)
                            || disableSwipe
                            || !selected.isEmpty()) {
                        return 0; // Don't swipe the headers.
                    }
                    return super.getSwipeDirs(recyclerView, viewHolder);
                }

                @Override
                public boolean onMove(
                    RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder,
                    RecyclerView.ViewHolder viewHolder1
                ) {
                    return false;
                }

                @Override
                public void onSwiped(
                    RecyclerView.ViewHolder viewHolder, int direction
                ) {
                    if(!selected.isEmpty())
                        return;
                    if(!(viewHolder instanceof FileViewHolder))
                        return;

                    PuzMetaFile puzMeta
                        = ((FileViewHolder) viewHolder).getPuzMetaFile();

                    fileHandlerProvider.get(fileHandler -> {
                    settings.getBrowseSwipeAction(action -> {
                        switch (action) {
                        case DELETE:
                            model.deletePuzzle(puzMeta);
                            break;
                        default:
                            if (model.getIsViewArchive()) {
                                model.movePuzzle(
                                    puzMeta,
                                    fileHandler.getCrosswordsDirectory()
                                );
                            } else {
                                model.movePuzzle(
                                    puzMeta,
                                    fileHandler.getArchiveDirectory()
                                );
                            }
                            break;
                        }
                    });});
                }
            });
        helper.attachToRecyclerView(binding.puzzleList);

        this.nm = NotificationManagerCompat.from(this);

        settings.liveBrowseSort().observe(this, accessor -> {
            loadPuzzleAdapter();
        });

        setupSpeedDial();

        model = new ViewModelProvider(this).get(BrowseActivityViewModel.class);
        model.getPuzzleFiles().observe(this, (v) -> {
            BrowseActivity.this.setViewCrosswordsOrArchiveUI();
            BrowseActivity.this.loadPuzzleAdapter();
            binding.swipeContainer.setRefreshing(false);
        });
        binding.setViewModel(model);
        binding.setLifecycleOwner(this);

        model.getIsUIBusy().observe(this, (isBusy) -> {
            if (isBusy)
                showPleaseWait();
            else
                hidePleaseWait();
        });

        model.getPuzzleLoadEvents().observe(this, (v) -> {
            Intent i = new Intent(BrowseActivity.this, PlayActivity.class);
            BrowseActivity.this.startActivity(i);
        });

        binding.swipeContainer.setOnRefreshListener(
             new SwipeRefreshLayout.OnRefreshListener() {
                 @Override
                 public void onRefresh() {
                     startLoadPuzzleList();
                 }
             }
         );

        setupPuzzleFilter();

        setViewCrosswordsOrArchiveUI();
        // populated properly inside onResume or with puzzle list
        // observer
        setPuzzleListAdapter(buildEmptyList(), false);

        checkNewVersion();

        // If this was started by a file open or a share
        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            // loaded by onResume
            setPendingImport(intent.getData());
        } else if (Intent.ACTION_SEND.equals(action)) {
            setPendingImport(IntentCompat.getParcelableExtra(
                intent, Intent.EXTRA_STREAM, Uri.class
            ));
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            setPendingImports(IntentCompat.getParcelableArrayListExtra(
                intent, Intent.EXTRA_STREAM, Uri.class
            ));
        }
    }

    private void setViewCrosswordsOrArchiveUI() {
        boolean viewArchive = model.getIsViewArchive();
        if (viewCrosswordsArchiveMenuItem != null) {
            viewCrosswordsArchiveMenuItem.setTitle(viewArchive
                ? BrowseActivity.this.getString(R.string.title_view_crosswords)
                : BrowseActivity.this.getString(R.string.title_view_archives)
            );
        }
        this.setTitle(viewArchive
            ? BrowseActivity.this.getString(R.string.title_view_archives)
            : BrowseActivity.this.getString(R.string.title_view_crosswords)
        );
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(closeActionReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // A background update will commonly happen when the user turns
        // on the preference for the first time, so check here to ensure
        // the UI is re-rendered when they exit the settings dialog.
        checkNewPuzzle(newPuzzle -> {
            if (
                model.getPuzzleFiles().getValue() == null
                || newPuzzle
            ) {
                if (hasPendingImports()) {
                    Collection<Uri> importUri = getPendingImports();
                    clearPendingImports();
                    onImportURIsAndFinish(importUri);

                    // won't be triggered by import if archive is shown
                    if (model.getIsViewArchive())
                        startLoadPuzzleList();
                } else {
                    startLoadPuzzleList();
                }
            } else {
                refreshLastAccessedPuzzle();
            }
            // previous game ended for now
            clearCurrentBoard();
        });

        autoDownloadIfEnabled();
        model.processToImportDirectory();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // permissions checks in onStart to avoid multiple calls via
        // onResume

        fileHandlerProvider.isMissingWritePermission(missing -> {
            if (missing) {
                boolean showRationale
                    = ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    );

                if (showRationale) {
                    DialogFragment dialog = new StoragePermissionDialog();
                    dialog.show(
                        getSupportFragmentManager(), "StoragePermissionDialog"
                    );
                } else {
                    requestWritePermission();
                }

                return;
            }
        });

        checkAutoDownloadNotificationPermissions();
    }

    public void checkNewPuzzle(Consumer<Boolean> cb) {
        fileHandlerProvider.isMissingWritePermission(missing -> {
            if (missing) {
                cb.accept(false);
            } else {
                settings.getBrowseNewPuzzle(newPuzzle -> {
                    clearNewPuzzleFlag();
                    cb.accept(newPuzzle);
                });
            }
        });
    }

    public void clearNewPuzzleFlag() {
        settings.setBrowseNewPuzzle(false);
    }



    private void refreshLastAccessedPuzzle() {
        final PuzHandle lastAccessed = getCurrentPuzHandle();
        if (lastAccessed == null)
            return;
        model.refreshPuzzleMeta(lastAccessed);
    }

    private SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter>
    buildEmptyList() {
        return new SeparatedRecyclerViewAdapter<>(
            R.layout.puzzle_list_header,
            FileViewHolder.class
        );
    }

    private SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter>
    buildList(
        List<MutableLiveData<PuzMetaFile>> puzFiles, Accessor accessor
    ) {
        try {
            Collections.sort(
                puzFiles,
                (pm1, pm2) -> {
                    return accessor.compare(pm1.getValue(), pm2.getValue());
                }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter> adapter
            = new SeparatedRecyclerViewAdapter<>(
                R.layout.puzzle_list_header,
                FileViewHolder.class
            );
        String lastHeader = null;
        ArrayList<MutableLiveData<PuzMetaFile>> current = new ArrayList<>();

        for (MutableLiveData<PuzMetaFile> pmData: puzFiles) {
            PuzMetaFile puzMeta = pmData.getValue();

            String check = accessor.getLabel(puzMeta);

            if (!((lastHeader == null) || lastHeader.equals(check))) {
                FileAdapter fa = new FileAdapter(current);
                adapter.addSection(lastHeader, fa);
                current = new ArrayList<>();
            }

            lastHeader = check;
            current.add(pmData);
        }

        if (lastHeader != null) {
            FileAdapter fa = new FileAdapter(current);
            adapter.addSection(lastHeader, fa);
        }

        return adapter;
    }

    private void checkAutoDownloadNotificationPermissions() {
        settings.getDownloadersSettings(settings -> {
        backgroundDownloadManager.isBackgroundDownloadEnabled(bgDownload -> {
            boolean downloading = bgDownload
                || settings.getDownloadOnStartup();

            if (settings.isNotificationPermissionNeeded() && downloading)
                checkRequestNotificationPermissions();
        });});
    }

    private void autoDownloadIfEnabled() {
        fileHandlerProvider.isMissingWritePermission(missing -> {
            if (missing) return;

            settings.getBrowseLastDownload(lastDL -> {
            settings.getDownloadersSettings(downloaderSettings -> {
            downloadersProvider.get(dls -> {
                long downloadCutoff
                    = System.currentTimeMillis() - (long) (12 * 60 * 60 * 1000);

                boolean isDownload = downloaderSettings.getDownloadOnStartup()
                    && downloadCutoff > lastDL;

                if (isDownload) {
                    model.download(LocalDate.now(), dls.getAutoDownloaders());
                    settings.setBrowseLastDownload(System.currentTimeMillis());
                }
            });});});
        });
    }

    private void startLoadPuzzleList() {
        startLoadPuzzleList(model.getIsViewArchive());
    }

    private void startLoadPuzzleList(boolean archive) {
        fileHandlerProvider.isMissingWritePermission(missing -> {
            if (missing) return;
            clearNewPuzzleFlag();
            model.startLoadFiles(archive);
        });
    }

    private void loadPuzzleAdapter() {
        cleanUpCurrentAdapter();
        List<MutableLiveData<PuzMetaFile>> puzList
            = model.getPuzzleFiles().getValue();
        if (puzList != null) {
            settings.getBrowseSort(accessor -> {
                setPuzzleListAdapter(buildList(puzList, accessor), true);
            });
        } else {
            setPuzzleListAdapter(buildEmptyList(), false);
        }
    }

    /**
     * Before changing adapter, clear up the old one
     */
    private void cleanUpCurrentAdapter() {
        if (currentAdapter != null) {
            for (FileAdapter adapter : currentAdapter.sectionAdapters()) {
                adapter.cleanUpForRemoval();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void onItemClick(final View v, final PuzMetaFile puzMeta) {
        if (!selected.isEmpty()) {
            updateSelection(v, puzMeta);
        } else {
            if (puzMeta == null)
                return;
            model.loadPuzzle(puzMeta);
        }
    }

    public void onItemLongClick(View v, PuzMetaFile puzMeta) {
        if (actionMode == null) {
            startSupportActionMode(actionModeCallback);
        }
        updateSelection(v, puzMeta);
    }

    private void updateSelection(View v, PuzMetaFile puzMeta) {
        if (selected.contains(puzMeta)) {
            setListItemColor(v, false);
            selected.remove(puzMeta);
        } else {
            setListItemColor(v, true);
            selected.add(puzMeta);
        }
        if (selected.isEmpty() && actionMode != null) {
            actionMode.finish();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void clearSelection() {
        selected.clear();
        currentAdapter.notifyDataSetChanged();
    }

    private boolean hasCurrentPuzzleListAdapter() {
        return currentAdapter != null;
    }

    /**
     * Set the puzzle list adapter
     * @param showEmptyMsgs give feedback to user when no files (used to
     * avoid doing so during loading)
     */
    private void setPuzzleListAdapter(
        SeparatedRecyclerViewAdapter<FileViewHolder, FileAdapter> adapter,
        boolean showEmptyMsgs
    ) {
        currentAdapter = adapter;
        binding.puzzleList.setAdapter(adapter);

        if (adapter.isEmpty() && showEmptyMsgs) {
            if (model.getIsViewArchive()) {
                binding.emptyListingMsg.setText(R.string.no_puzzles);
            } else {
                binding.emptyListingMsg.setText(
                    R.string.no_puzzles_download_or_configure_storage
                );
            }
            binding.emptyListingMsg.setVisibility(View.VISIBLE);

            settings.getFileHandlerSettings(fhSettings -> {
                if (fhSettings.getStorageLocation() == StorageLocation.INTERNAL)
                    binding.internalStorageMsg.setVisibility(View.VISIBLE);
                else
                    binding.internalStorageMsg.setVisibility(View.GONE);
            });
        } else {
            binding.emptyListingMsg.setVisibility(View.GONE);
            binding.internalStorageMsg.setVisibility(View.GONE);
        }

        showSpeedDial();
    }

    private void showPleaseWait() {
        binding.pleaseWaitNotice.setVisibility(View.VISIBLE);
        setSpeedDialVisibility(View.GONE);
    }

    private void hidePleaseWait() {
        binding.pleaseWaitNotice.setVisibility(View.GONE);
        setSpeedDialVisibility(View.VISIBLE);
    }

    private void setSpeedDialVisibility(int visibility) {
        binding.speedDialAdd.setVisibility(visibility);
    }

    /**
     * Unhide the FAB if hidden
     *
     * Distinct from setSpeedDialVisibility in that it uses the standard
     * scroll show/hide feature, rather than setting visibility directly
     */
    private void showSpeedDial() {
        binding.speedDialAdd.show();
    }

    private void setupSpeedDial() {
        binding.speedDialAdd.inflate(R.menu.speed_dial_browse_menu);

        binding.speedDialAdd.setOnActionSelectedListener(
            new SpeedDialView.OnActionSelectedListener() {
                @Override
                public boolean onActionSelected(
                    SpeedDialActionItem actionItem
                ) {
                    int id = actionItem.getId();
                    if (id == R.id.speed_dial_download) {
                        binding.speedDialAdd.close();
                        showDownloadDialog();
                        return true;
                    } else if (id == R.id.speed_dial_import) {
                        getImportURI.launch(IMPORT_MIME_TYPE);
                    } else if (id == R.id.speed_dial_online_sources) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(
                            Uri.parse(getString(R.string.online_sources_url))
                        );
                        startActivity(i);
                    }
                    return false;
                }
            }
        );

        setSpeedDialVisibility(View.VISIBLE);
    }

    /**
     * Import from URIs, does not force reload
     */
    private void onImportURIs(List<Uri> uris) {
        if (uris != null) {
            model.importURIs(uris, false, (someFailed, someSucceeded) -> {
                if (someSucceeded || someFailed) {
                    String msg = getString(
                        someFailed || !someSucceeded
                            ? R.string.import_failure
                            : R.string.import_success
                    );
                    Toast t = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
                    t.show();
                }
            });
        }
    }

    /**
     * Import from URI, force reload of puz list if asked
     */
    private void onImportURIsAndFinish(Collection<Uri> uri) {
        if (uri != null) {
            model.importURIs(uri, true, (someFailed, someSucceeded) -> {
                ImportedNowFinishDialog dialog = new ImportedNowFinishDialog();
                Bundle args = new Bundle();
                args.putBoolean(IMPORT_SUCCESS, !someFailed && someSucceeded);
                dialog.setArguments(args);
                dialog.show(
                    getSupportFragmentManager(), "ImportedNowFinishDialog"
                );
            });
        }
    }

    private boolean hasPendingImports() {
        return pendingImports != null;
    }

    private Collection<Uri> getPendingImports() {
        return pendingImports;
    }

    private void clearPendingImports() {
        pendingImports = null;
    }

    private void setPendingImport(Uri uri) {
        setPendingImports(Collections.singleton(uri));
    }

    private void setPendingImports(Collection<Uri> uri) {
        pendingImports = uri;
    }

    private void showDownloadDialog() {
        DialogFragment dialog = new DownloadDialog();
        checkAndWarnNetworkState();
        checkRequestNotificationPermissions();
        dialog.show(getSupportFragmentManager(), "DownloadDialog");
    }

    private void checkAndWarnNetworkState() {
        if (!utils.hasNetworkConnection(this)) {
            Toast t = Toast.makeText(
                this,
                R.string.download_but_no_active_network,
                Toast.LENGTH_LONG
            );
            t.show();
        }
    }

    /**
     * Request notification permissions if needed
     *
     * E.g. not if settings block them. Doesn't ask twice in an
     * activities life
     */
    private void checkRequestNotificationPermissions() {
        settings.getDownloadersSettings(settings -> {
            if (!settings.isNotificationPermissionNeeded())
                return;

            if (nm.areNotificationsEnabled())
                return;

            boolean showRationale
                = utils.shouldShowRequestNotificationPermissionRationale(this);

            if (showRationale) {
                Toast t = Toast.makeText(
                    this,
                    R.string.notifications_request_rationale,
                    Toast.LENGTH_LONG
                );
                t.show();
            }

            utils.requestPostNotifications(notificationPermissionLauncher);
        });
    }

    private void requestWritePermission() {
        writeStorageLauncher.launch(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
    }

    private void checkNewVersion() {
        settings.getBrowseLastSeenVersion(lastSeenVersion -> {
            String currentVersion = utils.getApplicationVersionName(this);

            if (!Objects.equals(currentVersion, lastSeenVersion)) {
                DialogFragment dialog = new NewVersionDialog();
                dialog.show(
                    getSupportFragmentManager(), "NewVersionDialog"
                );
            }

            settings.setBrowseLastSeenVersion(currentVersion);
        });
    }

    private void onActionBarWithText(MenuItem a) {
        a.setShowAsAction(
            MenuItem.SHOW_AS_ACTION_WITH_TEXT + MenuItem.SHOW_AS_ACTION_IF_ROOM
        );
    }

    private void setupPuzzleFilter() {
        binding.closeFilterButton.setOnClickListener(view -> {
            closePuzzleFilter();
        });

        String filterValue = model.getPuzzleFilter().getValue();
        boolean noFilter = filterValue == null || filterValue.isEmpty();
        binding.filterPanel.setVisibility(noFilter ? View.GONE : View.VISIBLE);
    }

    private void showPuzzleFilter() {
        model.getPuzzleFilter().setValue("");
        binding.filterPanel.setVisibility(View.VISIBLE);
    }

    private void closePuzzleFilter() {
        model.clearPuzzleFilter();
        binding.filterPanel.setVisibility(View.GONE);
    }

    private class FileAdapter
            extends RemovableRecyclerViewAdapter<FileViewHolder> {
        final ArrayList<MutableLiveData<PuzMetaFile>> objects;
        final Map<MutableLiveData<PuzMetaFile>, Observer<PuzMetaFile>>
            objectObservers = new HashMap<>();

        public FileAdapter(ArrayList<MutableLiveData<PuzMetaFile>> objects) {
            this.objects = objects;
            for (MutableLiveData<PuzMetaFile> pmData : objects) {
                addObserver(pmData);
            }
        }

        @Override
        public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            PuzzleListItemBinding itemBinding = PuzzleListItemBinding.inflate(
                inflater, parent, false
            );
            return new FileViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(FileViewHolder holder, int position) {
            MutableLiveData<PuzMetaFile> pmData = objects.get(position);
            PuzMetaFile pm = pmData.getValue();
            holder.setPuzMetaFile(pm);
        }

        @Override
        public int getItemCount() {
            return objects.size();
        }

        @Override
        public void remove(int position) {
            objects.remove(position);
        }

        /**
         * Call when adapter is about to be replaced
         *
         * Removes observers from all live data.
         */
        public void cleanUpForRemoval() {
            for (MutableLiveData<PuzMetaFile> pmData : objects)
                removeObserver(pmData);
        }

        /**
         * Only one observer per pmData, removes old if exists
         */
        private void addObserver(MutableLiveData<PuzMetaFile> pmData) {
            if (objectObservers.containsKey(pmData))
                removeObserver(pmData);

            Observer<PuzMetaFile> observer = (v) -> {
                // need to search each time since position may change
                // throughout lifecycle
                int idx = objects.indexOf(pmData);
                if (v == null) {
                    objects.remove(idx);
                    removeObserver(pmData);
                    FileAdapter.this.notifyItemRemoved(idx);
                } else {
                    FileAdapter.this.notifyItemChanged(idx);
                }
            };

            pmData.observe(BrowseActivity.this, observer);
            objectObservers.put(pmData, observer);
        }

        private void removeObserver(MutableLiveData<PuzMetaFile> pmData) {
            Observer<PuzMetaFile> observer = objectObservers.get(pmData);
            if (observer != null) {
                pmData.removeObserver(observer);
                objectObservers.remove(pmData);
            }
        }
    }

    private class FileViewHolder extends RecyclerView.ViewHolder {
        private PuzzleListItemBinding itemBinding;
        private PuzMetaFile puzMetaFile;

        public FileViewHolder(PuzzleListItemBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;

            settings.liveBrowseSort().observe(
                BrowseActivity.this,
                accessor -> {
                    if (accessor == Accessor.SOURCE) {
                        itemBinding.puzzleDate.setVisibility(View.VISIBLE);
                    } else {
                        itemBinding.puzzleDate.setVisibility(View.GONE);
                    }
                }
            );
        }

        public void setPuzMetaFile(PuzMetaFile puzMetaFile) {
            this.puzMetaFile = puzMetaFile;

            itemBinding.getRoot().setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        BrowseActivity.this.onItemClick(view, puzMetaFile);
                    }
                }
            );

            itemBinding.getRoot().setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        BrowseActivity.this.onItemLongClick(
                            view, puzMetaFile
                        );
                        return true;
                    }
                }
            );

            itemBinding.puzzleDate.setText(
                DATE_FORMAT.format(puzMetaFile.getDate())
            );

            String title = puzMetaFile.getTitle();
            String caption = puzMetaFile.getCaption();
            String author = puzMetaFile.getAuthor();

            itemBinding.puzzleName.setText(smartHtml(title));

            itemBinding.puzzleProgress.setPercentFilled(
                puzMetaFile.getFilled()
            );
            itemBinding.puzzleProgress.setComplete(
                puzMetaFile.getComplete() == 100
            );

            // add author if not already in title or caption
            // case insensitive trick:
            // https://www.baeldung.com/java-case-insensitive-string-matching
            String quotedAuthor = Pattern.quote(author);
            boolean addAuthor
                = author.length() > 0
                    && !title.matches("(?i).*" + quotedAuthor + ".*")
                    && !caption.matches("(?i).*" + quotedAuthor + ".*");

            if (addAuthor) {
                itemBinding.puzzleCaption.setText(smartHtml(
                    itemBinding.getRoot().getContext().getString(
                        R.string.puzzle_caption_with_author, caption, author
                    )
                ));
            } else {
                itemBinding.puzzleCaption.setText(smartHtml(caption));
            }

            setListItemColor(
                itemBinding.getRoot(), selected.contains(puzMetaFile)
            );
        }

        public PuzMetaFile getPuzMetaFile() {
            return puzMetaFile;
        }
    }

    @AndroidEntryPoint
    public static class DownloadDialog extends DialogFragment {
        @Inject
        DownloadersProvider downloadersProvider;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            DownloadPickerDialogBuilder.OnDownloadSelectedListener
                downloadButtonListener
                    = new DownloadPickerDialogBuilder
                        .OnDownloadSelectedListener() {
                public void onDownloadSelected(
                    LocalDate d,
                    List<Downloader> downloaders
                ) {
                    BrowseActivityViewModel model
                        = new ViewModelProvider(getActivity())
                            .get(BrowseActivityViewModel.class);

                    model.download(d, downloaders);
                }
            };

            LocalDate d = LocalDate.now();
            BrowseActivity activity = (BrowseActivity) getActivity();

            DownloadPickerDialogBuilder dpd
                = new DownloadPickerDialogBuilder(
                    activity,
                    downloadButtonListener,
                    d.getYear(),
                    d.getMonthValue(),
                    d.getDayOfMonth(),
                    downloadersProvider
            );

            return dpd.getInstance();
        }
    }

    public static class NotificationPermissionDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());

            builder.setTitle(getString(R.string.disable_notifications))
                .setMessage(getString(R.string.notifications_denied_msg))
                .setPositiveButton(
                    R.string.disable_notifications_button,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            DialogInterface dialogInterface, int i
                        ) {
                            BrowseActivity activity
                                = (BrowseActivity) getActivity();
                            activity.settings.disableNotifications();
                        }
                    }
                ).setNegativeButton(
                    R.string.android_app_settings_button,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            DialogInterface dialogInterface, int i
                        ) {
                            String appPackage = getActivity().getPackageName();
                            Intent intent = new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + appPackage)
                            );
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                );

            return builder.create();
        }
    }

    public static class StoragePermissionDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            BrowseActivity activity = (BrowseActivity) getActivity();

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());

            builder.setTitle(R.string.allow_permissions)
                .setMessage(R.string.please_allow_storage)
                .setPositiveButton(
                    android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            DialogInterface dialogInterface, int i
                        ) {
                            activity.requestWritePermission();
                        }
                    }
                );

            return builder.create();
        }
    }

    @AndroidEntryPoint
    public static class NewVersionDialog extends DialogFragment {

        @Inject
        protected AndroidVersionUtils utils;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();

            String versionName
                = utils.getApplicationVersionName(activity);
            String title
                = getString(R.string.new_version_title, versionName);

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(activity);

            builder.setTitle(title)
                .setMessage(R.string.new_version_message)
                // again with apologies to material guidelines
                .setPositiveButton(
                    R.string.view_release_notes,
                    (dialogInterface, i) -> {
                        Intent intent = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("release.html"),
                            activity,
                            HTMLActivity.class
                        );
                        activity.startActivity(intent);
                    }
                ).setNegativeButton(R.string.close, null);

            return builder.create();
        }
    }

    @AndroidEntryPoint
    public static class ImportedNowFinishDialog extends DialogFragment {

        @Inject
        protected ForkyzSettings settings;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            BrowseActivity activity = (BrowseActivity) getActivity();

            Bundle args = getArguments();
            boolean success = false;
            if (args != null)
                success = args.getBoolean(IMPORT_SUCCESS, false);

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());

            int message = success
                ? R.string.import_success_long
                : R.string.import_failure_long;

            // even if some failed, there might still be a new one
            settings.setBrowseNewPuzzle(true);

            builder.setTitle(R.string.imports_title)
                .setMessage(message)
                .setPositiveButton(
                    R.string.exit,
                    (di, i) -> { activity.finish(); }
                );

            return builder.create();
        }

        @Override
        public void onCancel(DialogInterface di) {
            getActivity().finish();
        }
    }

}
