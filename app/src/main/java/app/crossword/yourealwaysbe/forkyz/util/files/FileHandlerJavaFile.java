
package app.crossword.yourealwaysbe.forkyz.util.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.logging.Logger;

import android.content.Context;
import android.net.Uri;

/**
 * Base implementation of java.io.File-based file access from a given
 * root directory.
 */
public abstract class FileHandlerJavaFile extends FileHandler {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerJavaFile.class.getCanonicalName());

    private File rootDirectory;

    public FileHandlerJavaFile(
        Context applicationContext, File rootDirectory
    ) {
        super(applicationContext);
        this.rootDirectory = rootDirectory;
    }

    /**
     * For testing purposes, allow a meta cache to be passed in
     */
    public FileHandlerJavaFile(
        Context applicationContext, MetaCache metaCache, File rootDirectory
    ) {
        super(applicationContext, metaCache);
        this.rootDirectory = rootDirectory;
    }

    @Override
    public DirHandle getCrosswordsDirectory() {
        return getGenericDirectory("crosswords");
    }

    @Override
    public DirHandle getArchiveDirectory() {
        return getGenericDirectory("crosswords/archive");
    }

    @Override
    public DirHandle getToImportDirectory() {
        return getGenericDirectory("crosswords/to-import");
    }

    @Override
    public DirHandle getToImportDoneDirectory() {
        return getGenericDirectory("crosswords/to-import-done");
    }

    @Override
    public DirHandle getToImportFailedDirectory() {
        return getGenericDirectory("crosswords/to-import-failed");
    }

    @Override
    public Iterable<FileHandle> listFiles(final DirHandle dir) {
        return new Iterable<FileHandle>() {
            public Iterator<FileHandle> iterator() {
                return new Iterator<FileHandle>() {
                    private int pos = 0;
                    private File[] files = dir.getFile().listFiles();
                    private File next = getNext();

                    public boolean hasNext() {
                        return next != null;
                    }

                    public FileHandle next() {
                        File result = next;
                        next = getNext();
                        return new FileHandle(result);
                    }

                    private File getNext() {
                        if (files == null)
                            return null;

                        while (pos < files.length) {
                            next = files[pos++];
                            if (next.isFile())
                                return next;
                        }

                        return null;
                    }
                };
            }
        };
    }

    @Override
    public String getName(FileHandle f) {
        return f.getFile().getName();
    }

    @Override
    protected FileHandle getFileHandle(Uri uri) {
        return new FileHandle(new File(uri.getPath()));
    }

    @Override
    protected boolean exists(DirHandle dir) {
        return dir.getFile().exists();
    }

    @Override
    protected boolean exists(FileHandle file) {
        return file.getFile().exists();
    }

    @Override
    protected Uri getUri(DirHandle d) {
        return d.getUri();
    }

    @Override
    protected Uri getUri(FileHandle f) {
        return f.getUri();
    }

    @Override
    protected long getLastModified(FileHandle file) {
        return file.getFile().lastModified();
    }

    @Override
    protected FileHandle createFileHandle(
        DirHandle dir, String fileName, String mimeType
    ) {
        File file = new File(dir.getFile(), fileName);
        if (file.exists())
            return null;
        return new FileHandle(new File(dir.getFile(), fileName));
    }

    @Override
    protected void deleteUnsync(FileHandle fileHandle) {
        if (exists(fileHandle))
            fileHandle.getFile().delete();
    }

    @Override
    protected void moveToUnsync(
        FileHandle fileHandle, DirHandle srcDirHandle, DirHandle destDirHandle
    ) {
        File file = fileHandle.getFile();
        File directory = destDirHandle.getFile();
        File newFile = getUniqueFile(directory, file.getName());
        file.renameTo(newFile);
    }

    @Override
    protected OutputStream getOutputStream(FileHandle fileHandle)
            throws IOException {
        return new FileOutputStream(fileHandle.getFile());
    }

    @Override
    protected InputStream getInputStream(FileHandle fileHandle)
            throws IOException {
        return new FileInputStream(fileHandle.getFile());
    }

    private File getRootDirectory() {
        return rootDirectory;
    }

    private DirHandle getGenericDirectory(String path) {
        File dir = new File(getRootDirectory(), path);
        dir.mkdirs();
        return new DirHandle(dir);
    }

    /**
     * Get a unique file name for the directory
     *
     * E.g. dir/name.txt comes back as dir/name (1).txt if dir/name.txt
     * already exists
     */
    private File getUniqueFile(File directory, String name) {
        File newFile = new File(directory, name);

        int counter = 1;
        String basename = null;
        String extension = null;

        while (newFile.exists()) {
            if (basename == null) {
                int i = name.lastIndexOf(".");
                if (i >= 0) {
                    basename = name.substring(0, i);
                    extension = name.substring(i);
                } else {
                    basename = name;
                    extension = "";
                }
            }
            String newName = basename + " (" + counter + ")" + extension;

            newFile = new File(directory, newName);
        }

        return newFile;
    }
}
