
package app.crossword.yourealwaysbe.forkyz.util.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract;
import android.util.Log;

import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation;
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils;
import app.crossword.yourealwaysbe.puz.io.StreamUtils;

public class FileHandlerSAF extends FileHandler {
    private static final String TAG = "ForkyzFileHandlerSAF";

    private static final String ARCHIVE_NAME = "archive";
    private static final String TO_IMPORT_NAME = "to-import";
    private static final String TO_IMPORT_DONE_NAME = "to-import-done";
    private static final String TO_IMPORT_FAILED_NAME = "to-import-failed";

    private Context context;
    private Uri rootUri;
    private Uri crosswordsFolderUri;
    private Uri archiveFolderUri;
    private Uri toImportFolderUri;
    private Uri toImportDoneFolderUri;
    private Uri toImportFailedFolderUri;

    public static class Meta {
        private String name;
        private long lastModified;
        private String mimeType;

        public Meta(String name, long lastModified, String mimeType) {
            this.name = name;
            this.lastModified = lastModified;
            this.mimeType = mimeType;
        }

        public String getName() { return name; }
        public long getLastModified() { return lastModified; }
        public String getMimeType() { return mimeType; }
    }

    /**
     * Construct FileHandler from context and folder URIs
     *
     * Context should be an application context, not an activity that
     * may go out of date.
     *
     * @param rootUri the tree the user has granted permission to
     * @param crosswordsFolderUri the document uri for the crosswords
     * folder
     * @param archiveFolderUri the document uri for the archive folder
     */
    public FileHandlerSAF(
        Context context,
        NativeBackendUtils utils,
        Uri rootUri,
        Uri crosswordsFolderUri,
        Uri archiveFolderUri,
        Uri toImportFolderUri,
        Uri toImportDoneFolderUri,
        Uri toImportFailedFolderUri
    ) {
        super(utils);
        this.context = context;
        this.rootUri = rootUri;
        this.crosswordsFolderUri = crosswordsFolderUri;
        this.archiveFolderUri = archiveFolderUri;
        this.toImportFolderUri = toImportFolderUri;
        this.toImportDoneFolderUri = toImportDoneFolderUri;
        this.toImportFailedFolderUri = toImportFailedFolderUri;
    }

    @Override
    public DirHandle getCrosswordsDirectory() {
        return new DirHandle(crosswordsFolderUri);
    }

    @Override
    public DirHandle getArchiveDirectory() {
        return new DirHandle(archiveFolderUri);
    }

    @Override
    public DirHandle getToImportDirectory() {
        return new DirHandle(toImportFolderUri);
    }

    @Override
    public DirHandle getToImportDoneDirectory() {
        return new DirHandle(toImportDoneFolderUri);
    }

    @Override
    public DirHandle getToImportFailedDirectory() {
        return new DirHandle(toImportFailedFolderUri);
    }

    @Override
    public Iterable<FileHandle> listFiles(DirHandle dir) {
        ContentResolver resolver = getContentResolver();
        Uri dirUri = dir.getUri();
        String dirTreeId = DocumentsContract.getDocumentId(dirUri);
        Uri dirTreeUri = DocumentsContract.buildDocumentUriUsingTree(
            rootUri, dirTreeId
        );

        Uri childrenUri
            = DocumentsContract.buildChildDocumentsUriUsingTree(
                dirTreeUri, dirTreeId
            );

        ArrayList<FileHandle> files = new ArrayList<>();

        try (
            Cursor cursor = resolver.query(
                childrenUri,
                new String[] {
                    Document.COLUMN_DOCUMENT_ID,
                    Document.COLUMN_DISPLAY_NAME,
                    Document.COLUMN_LAST_MODIFIED,
                    Document.COLUMN_MIME_TYPE
                },
                null, null, null
            )
        ) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String name = cursor.getString(1);
                long modified = cursor.getLong(2);
                String mimeType = cursor.getString(3);

                if (!Document.MIME_TYPE_DIR.equals(mimeType)) {
                    Uri uri = DocumentsContract.buildDocumentUriUsingTree(
                        dirUri, id
                    );

                    files.add(
                        new FileHandle(uri, new Meta(name, modified, mimeType))
                    );
                }
            }
        }

        return files;
    }

    @Override
    public String getName(FileHandle f) {
        return f.getSAFMeta().getName();
    }

    @Override
    protected FileHandle getFileHandle(Uri uri) {
        Meta meta = getMetaFromUri(uri);
        if (meta != null)
            return new FileHandle(uri, meta);
        else
            return null;
    }

    @Override
    protected boolean exists(DirHandle dir) {
        return exists(getContentResolver(), dir.getUri());
    }

    @Override
    protected boolean exists(FileHandle file) {
        return exists(getContentResolver(), file.getUri());
    }

    @Override
    protected Uri getUri(DirHandle f) { return f.getUri(); }

    @Override
    protected Uri getUri(FileHandle f) { return f.getUri(); }

    @Override
    protected long getLastModified(FileHandle file) {
        return file.getSAFMeta().getLastModified();
    }

    @Override
    protected void deleteUnsync(FileHandle fileHandle) {
        try {
            DocumentsContract.deleteDocument(
                getContentResolver(),
                fileHandle.getUri()
            );
        } catch (FileNotFoundException e) {
            // seems like our work is done
        } catch (IllegalArgumentException e) {
            // if the file does not exist, this might be thrown since
            // Android cannot determine access permissions
            if (e.getCause() instanceof FileNotFoundException) {
                // ignore
            } else {
                throw e;
            }
        }
    }

    @Override
    protected void moveToUnsync(
        FileHandle fileHandle, DirHandle srcDirHandle, DirHandle destDirHandle
    ) {
        // outside of android version utils but awkward to move
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            moveToUnsyncLegacy(fileHandle, srcDirHandle, destDirHandle);
        } else {
            try {
                DocumentsContract.moveDocument(
                    getContentResolver(),
                    fileHandle.getUri(),
                    srcDirHandle.getUri(),
                    destDirHandle.getUri()
                );
            } catch (FileNotFoundException | IllegalArgumentException e) {
                Log.e(
                    TAG,
                    "Attempt to move " + fileHandle + " to "
                        + destDirHandle + " failed."
                );
                e.printStackTrace();
            } catch (IllegalStateException e) {
                // assume name clash and fall back to legacy manual copy
                // its createDocument will choose a new name if needed
                moveToUnsyncLegacy(fileHandle, srcDirHandle, destDirHandle);
            }
        }
    }

    private void moveToUnsyncLegacy(
        FileHandle fileHandle, DirHandle srcDirHandle, DirHandle destDirHandle
    ) {
        try {
            Uri newUri = DocumentsContract.createDocument(
                getContentResolver(),
                destDirHandle.getUri(),
                getMimeType(fileHandle),
                getName(fileHandle)
            );
            try (
                InputStream is = getBufferedInputStream(fileHandle);
                OutputStream os
                    = getBufferedOutputStream(getFileHandle(newUri))
            ) {
                StreamUtils.copyStream(is, os);
                DocumentsContract.deleteDocument(
                    getContentResolver(), fileHandle.getUri()
                );
            }
        } catch (
            IllegalArgumentException
            | IllegalStateException
            | IOException e
        ) {
            Log.e(
                TAG,
                "Attempt to move " + fileHandle + " to "
                   + destDirHandle + " failed."
            );
            e.printStackTrace();
        }
    }

    @Override
    protected OutputStream getOutputStream(FileHandle fileHandle)
        throws IOException {
        try {
            return getContentResolver().openOutputStream(
                fileHandle.getUri(), "wt"
            );
        } catch (IllegalArgumentException e) {
            // happens when e.g. file was deleted, so consider as IO
            throw new IOException(e);
        }
    }

    @Override
    protected InputStream getInputStream(FileHandle fileHandle)
        throws IOException {
        try {
            return getContentResolver().openInputStream(
                fileHandle.getUri()
            );
        } catch (IllegalArgumentException e) {
            // happens when e.g. file was deleted, so consider as IO
            throw new IOException(e);
        }
    }

    @Override
    public boolean isStorageMounted() {
        ContentResolver resolver = getContentResolver();
        try {
            return exists(resolver, crosswordsFolderUri)
                && exists(resolver, archiveFolderUri);
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "Unsupported operation accessing SAF");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isStorageFull() {
        // In general storage size doesn't make sense for SAF (e.g. size
        // of cloud storage). There is a method of getting space [1] for
        // local directories, but it delays start up by about 0.5s,
        // which is noticably laggy.
        //
        // [1]: https://stackoverflow.com/a/40848958
        return false;
    }

    @Override
    FileHandle createFileHandle(
        DirHandle dir, String fileName, String mimeType
    ) {
        try {
            mimeType = (mimeType == null) ? MIME_TYPE_GENERIC : mimeType;
            Uri uri = DocumentsContract.createDocument(
                getContentResolver(), dir.getUri(), mimeType, fileName
            );
            if (uri != null) {
                return new FileHandle(
                    uri,
                    new Meta(fileName, System.currentTimeMillis(), mimeType)
                );
            }
        } catch (FileNotFoundException e) {
            // fall through
        }

        return null;
    }

    /**
     * Convenience for same with keeping permission
     */
    public static FileHandlerSAF initialiseSAFForRoot(
        Context applicationContext,
        NativeBackendUtils utils,
        Uri rootUri
    ) {
        return initialiseSAFForRoot(applicationContext, utils, rootUri, true);
    }

    /**
     * Initialise a crosswords directory in the give rootUri
     *
     * This will search the contents of rootUri to see if directories
     * already exist (and use them if so). If not, it will create the
     * required folders. It will keep the permission for the Uri if all
     * is well.
     *
     * Once this has been called, the file handler will need saving to
     * settings for readHandlerFromSettings to work
     *
     * @param applicationContext the application context
     * @param utils native backend utils
     * @param rootUri the root permitted folder for storage
     * @param keepPermission whether to keep access permission to
     * rootUri
     * @return an initiated file handler if successful, else null
     */
    public static FileHandlerSAF initialiseSAFForRoot(
        Context applicationContext,
        NativeBackendUtils utils,
        Uri rootUri,
        boolean keepPermission
    ) {
        try {
            ContentResolver resolver = applicationContext.getContentResolver();
            String dirId
                = DocumentsContract.getTreeDocumentId(rootUri);

            Uri crosswordsFolderUri
                = DocumentsContract.buildDocumentUriUsingTree(
                    rootUri, dirId
                );

            Uri[] subDirs = searchCreateDirectories(
                new String[] {
                    ARCHIVE_NAME,
                    TO_IMPORT_NAME,
                    TO_IMPORT_DONE_NAME,
                    TO_IMPORT_FAILED_NAME
                },
                rootUri,
                resolver
            );

            Uri archiveFolderUri = subDirs[0];
            Uri toImportFolderUri = subDirs[1];
            Uri toImportDoneFolderUri = subDirs[2];
            Uri toImportFailedFolderUri = subDirs[3];

            // if all ok, keep permission
            if (
                crosswordsFolderUri != null
                && archiveFolderUri != null
                && toImportFolderUri != null
                && toImportDoneFolderUri != null
                && toImportFailedFolderUri != null
            ) {
                if (keepPermission) {
                    // persist permissions
                    int takeFlags = (
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );
                    resolver.takePersistableUriPermission(rootUri, takeFlags);
                }

                return new FileHandlerSAF(
                    applicationContext,
                    utils,
                    rootUri,
                    crosswordsFolderUri,
                    archiveFolderUri,
                    toImportFolderUri,
                    toImportDoneFolderUri,
                    toImportFailedFolderUri
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to (re-)configure SAF directory.");
        }

        return null;
    }

    /**
     * Return a settings object for the file handler
     */
    @Override
    public FileHandlerSettings getSettings() {
        return new FileHandlerSettings(
            StorageLocation.SL_EXTERNAL_SAF,
            rootUri.toString(),
            crosswordsFolderUri.toString(),
            archiveFolderUri.toString(),
            toImportFolderUri.toString(),
            toImportDoneFolderUri.toString(),
            toImportFailedFolderUri.toString()
        );
    }

    /**
     * Read handler using locations stored in settings.
     *
     * Requires initialiseSAFSettings to have been called first. Returns
     * null if the handler could not be created. (E.g. if there are no
     * configured directories.) Will reinitialise if the rootUri is
     * still available.
     *
     * If reinit is needed, the settings are updated with the refreshed
     * values
     */
    public static FileHandlerSAF readHandlerFromSettings(
        Context applicationContext,
        NativeBackendUtils utils,
        FileHandlerSettings fileHandlerSettings,
        ForkyzSettings settings
    ) {
        FileHandlerSAF fileHandler = null;

        String rootFolder = fileHandlerSettings.getSafRootURI();
        String crosswordsFolder = fileHandlerSettings.getSafCrosswordsURI();
        String archiveFolder = fileHandlerSettings.getSafArchiveURI();
        String toImportFolder = fileHandlerSettings.getSafToImportURI();
        String toImportDoneFolder = fileHandlerSettings.getSafToImportDoneURI();
        String toImportFailedFolder
            = fileHandlerSettings.getSafToImportFailedURI();

        // actually never null since Kotlin, but hey
        if (
            rootFolder != null && !rootFolder.isEmpty()
            && crosswordsFolder != null && !crosswordsFolder.isEmpty()
            && archiveFolder != null && !archiveFolder.isEmpty()
            && toImportFolder != null && !toImportFolder.isEmpty()
            && toImportDoneFolder != null && !toImportDoneFolder.isEmpty()
            && toImportFailedFolder != null && toImportFailedFolder.isEmpty()
        ) {
            Uri rootFolderUri = Uri.parse(rootFolder);
            Uri crosswordsFolderUri = Uri.parse(crosswordsFolder);
            Uri archiveFolderUri = Uri.parse(archiveFolder);
            Uri toImportFolderUri = Uri.parse(toImportFolder);
            Uri toImportDoneFolderUri = Uri.parse(toImportDoneFolder);
            Uri toImportFailedFolderUri = Uri.parse(toImportFailedFolder);

            ContentResolver resolver = applicationContext.getContentResolver();

            try {
                if (
                    exists(resolver, crosswordsFolderUri)
                    && exists(resolver, archiveFolderUri)
                    && exists(resolver, toImportFolderUri)
                    && exists(resolver, toImportDoneFolderUri)
                    && exists(resolver, toImportFailedFolderUri)
                ) {
                    fileHandler = new FileHandlerSAF(
                        applicationContext,
                        utils,
                        rootFolderUri,
                        crosswordsFolderUri,
                        archiveFolderUri,
                        toImportFolderUri,
                        toImportDoneFolderUri,
                        toImportFailedFolderUri
                    );
                }
            } catch (SecurityException e) {
                Log.e(
                    TAG,
                    "Permission not granted to configured SAF directories."
                );
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "Unsupported operation with SAF");
                e.printStackTrace();
            }
        }

        if (
            fileHandler == null
            && rootFolder != null
            && !rootFolder.isEmpty()
        ) {
            fileHandler = initialiseSAFForRoot(
                applicationContext, utils, Uri.parse(rootFolder)
            );
            // update settings to good values
            if (fileHandler != null && settings != null) {
                settings.setFileHandlerSettings(fileHandler.getSettings(), null);
            }
        }

        return fileHandler;
    }

    private static boolean exists(ContentResolver resolver, Uri uri) {
        try (
            Cursor c = resolver.query(
                uri,
                new String[] {
                    Document.COLUMN_DOCUMENT_ID
                },
                null, null, null
            )
        ) {
            return c.getCount() > 0;
        } catch (IllegalArgumentException e) {
            // if the file does not exist, this is thrown
            return false;
        }
    }

    private Meta getMetaFromUri(Uri uri) {
        try (
            Cursor c = getContentResolver().query(
                uri,
                new String[] {
                    Document.COLUMN_DISPLAY_NAME,
                    Document.COLUMN_LAST_MODIFIED,
                    Document.COLUMN_MIME_TYPE
                },
                null, null, null
            )
        ) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                return new Meta(
                    c.getString(0),
                    // avoid exception crash if last modified is not known
                    // e.g. when opening firefox download urls
                    getLongColumnWithDefault(c, 1, System.currentTimeMillis()),
                    c.getString(2)
                );
            } else {
                return null;
            }
        }
    }

    private long getLongColumnWithDefault(
        Cursor c, int columnIndex, long defaultValue
    ) {
        try {
            return c.getLong(columnIndex);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    private ContentResolver getContentResolver() {
        return context.getContentResolver();
    }

    public String getMimeType(FileHandle f) {
        return f.getSAFMeta().getMimeType();
    }

    /**
     * Search for named subfolders in dirUri or create
     *
     * Throws exception if fails.
     *
     * @param names the names of the desired directories
     * @param baseUri the actual baseUri
     * @param resolver a content resolver
     * @return an array of the found/created Uris
     */
    private static Uri[] searchCreateDirectories(
        String[] names, Uri baseUri, ContentResolver resolver
    ) throws FileNotFoundException {
        String dirId
            = DocumentsContract.getTreeDocumentId(baseUri);
        Uri dirUri
            = DocumentsContract.buildDocumentUriUsingTree(
                baseUri, dirId
            );
        Uri childrenUri
            = DocumentsContract.buildChildDocumentsUriUsingTree(
                baseUri, dirId
            );

        // do a search
        Uri[] desiredUris = new Uri[names.length];
        try (
            // would be nice to select only folders, but selection args
            // are ignored atmo
            // https://stackoverflow.com/a/61214849/6882587
            Cursor cursor = resolver.query(
                childrenUri,
                new String[] {
                    Document.COLUMN_DISPLAY_NAME,
                    Document.COLUMN_MIME_TYPE,
                    Document.COLUMN_DOCUMENT_ID
                },
                null, null, null
            )
        ) {
            while (cursor.moveToNext()) {
                String curName = cursor.getString(0);
                String curMimeType = cursor.getString(1);
                String curID = cursor.getString(2);

                if (Document.MIME_TYPE_DIR.equals(curMimeType)) {
                    for (int i = 0; i < names.length; i++) {
                        if (names[i].equals(curName)) {
                            desiredUris[i]
                                = DocumentsContract.buildDocumentUriUsingTree(
                                    baseUri, curID
                                );
                        }
                    }
                }
            }
        }

        // if not found, create new
        for (int i = 0; i < names.length; i++) {
            if (desiredUris[i] == null) {
                desiredUris[i] = DocumentsContract.createDocument(
                    resolver, dirUri, Document.MIME_TYPE_DIR, names[i]
                );
            }
        }

        return desiredUris;
    }
}
