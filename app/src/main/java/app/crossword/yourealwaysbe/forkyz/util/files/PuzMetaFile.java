
package app.crossword.yourealwaysbe.forkyz.util.files;

import java.time.LocalDate;

import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils;

public class PuzMetaFile
        implements Comparable<PuzMetaFile> {
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

    public PuzHandle getPuzHandle() { return handle; }

    public int compareTo(PuzMetaFile other) {
        try {
            // because LocalDate is day-month-year, fall back to name
            int dateCmp = other.getDate().compareTo(this.getDate());
            if (dateCmp != 0)
                return dateCmp;
            return getHandler().getName(
                this.handle.getMainFileHandle()
            ).compareTo(
                getHandler().getName(
                    other.handle.getMainFileHandle()
                )
            );
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * True if the objects refer to the same underlying puzzle file
     */
    public boolean isSameMainFile(PuzHandle other) {
        return getPuzHandle().isSameMainFile(other);
    }

    /**
     * True if the objects refer to the same underlying puzzle file
     */
    public boolean isSameMainFile(PuzMetaFile other) {
        return isSameMainFile(other.getPuzHandle());
    }

    public boolean isInDirectory(DirHandle dirHandle) {
        return getPuzHandle().isInDirectory(dirHandle);
    }

    public String getCaption() {
        String caption = (meta == null) ? "" : meta.getTitle();
        return caption == null ? "" : caption;
    }

    public LocalDate getDate() {
        LocalDate date = null;
        if (meta != null) {
            String fileName = getHandler().getName(handle.getMainFileHandle());
            date = AppPuzzleUtils.deriveDate(meta.getDate(), fileName);
        }

        if (date == null)
            date = getHandler().getModifiedDate(handle.getMainFileHandle());

        return date;
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
        if (meta != null) {
            String fileName = getHandler().getName(handle.getMainFileHandle());
            String source = AppPuzzleUtils.deriveSource(
                meta.getSource(), fileName, meta.getAuthor(), meta.getTitle()
            );
            if (source != null)
                return source;
        }
        return "Unknown";
    }

    public String getTitle() {
        if ((meta == null)
                || (meta.getSource() == null)
                || (meta.getSource().length() == 0)) {
            String fileName = getHandler().getName(handle.getMainFileHandle());
            return fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            return meta.getSource();
        }
    }

    public String getAuthor() {
        String author = (meta == null) ? null : meta.getAuthor();
        return (author == null) ? "" : author;
    }

    @Override
    public String toString(){
        return getHandler().getUri(handle.getMainFileHandle()).toString();
    }

    private FileHandler getHandler() {
        return fileHandler;
    }
}
