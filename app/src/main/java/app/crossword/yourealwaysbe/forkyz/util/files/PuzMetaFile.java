
package app.crossword.yourealwaysbe.forkyz.util.files;

import java.time.LocalDate;

public class PuzMetaFile {
    private FileHandler fileHandler;

    public PuzHandle handle;
    public MetaCache.MetaRecord meta;

    PuzMetaFile(
        FileHandler fileHandler, PuzHandle handle, MetaCache.MetaRecord meta
    ) {
        this.fileHandler = fileHandler;
        this.handle = handle;
        this.meta = meta;
    }

    /**
     * A unique ID for the puzzle
     */
    public String getMainFileURI() {
        return getHandler()
            .getUri(getPuzHandle().getMainFileHandle())
            .toString();
    }

    public String getFileName() {
        return getHandler().getName(handle.getMainFileHandle());
    }

    public String getFileNameNoExtension() {
        String fileName = getFileName();
        return fileName == null
            ? null
            : fileName.substring(0, fileName.lastIndexOf("."));
    }

    public PuzHandle getPuzHandle() { return handle; }

    public String getTitle() {
        return (meta == null) ? null : meta.getTitle();
    }

    public LocalDate getDate() {
        return meta == null ? null : meta.getDate();
    }

    public LocalDate getFileModifiedDate() {
        return getHandler().getModifiedDate(handle.getMainFileHandle());
    }

    public int getComplete() {
        return (meta == null) ? 0 : meta.getPercentComplete();
    }

    public int getFilled() {
        return (meta == null) ? 0 : meta.getPercentFilled();
    }

    public char getRating() {
        return (meta == null) ? 0 : meta.getRating();
    }

    public boolean hasSolution() {
        return (meta == null) ? false : meta.hasSolution();
    }

    /**
     * Returns something that can be used as source
     *
     * May fall back to derived alternatives if no true source set.
     */
    public String getSource() {
        return meta == null ? null : meta.getSource();
    }

    public String getAuthor() {
        return (meta == null) ? null : meta.getAuthor();
    }

    @Override
    public String toString(){
        return getHandler().getUri(handle.getMainFileHandle()).toString();
    }

    private FileHandler getHandler() {
        return fileHandler;
    }
}
