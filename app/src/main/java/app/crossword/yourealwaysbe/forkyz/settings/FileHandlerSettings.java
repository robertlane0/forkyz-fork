
package app.crossword.yourealwaysbe.forkyz.settings;

// TODO: convert to record class when possible
public class FileHandlerSettings {
    private StorageLocation storageLocation;
    private String safRootURI;
    private String safCrosswordsURI;
    private String safArchiveURI;
    private String safToImportURI;
    private String safToImportDoneURI;
    private String safToImportFailedURI;

    public FileHandlerSettings(
        StorageLocation storageLocation,
        String safRootURI,
        String safCrosswordsURI,
        String safArchiveURI,
        String toImportURI,
        String toImportDoneURI,
        String toImportFailedURI
    ) {
        this.storageLocation = storageLocation;
        this.safRootURI = safRootURI;
        this.safCrosswordsURI = safCrosswordsURI;
        this.safArchiveURI = safArchiveURI;
        this.safToImportURI = toImportURI;
        this.safToImportDoneURI = toImportDoneURI;
        this.safToImportFailedURI = toImportFailedURI;
    }

    public StorageLocation getStorageLocation() { return storageLocation; }
    public String getSAFRootURI() { return safRootURI; }
    public String getSAFCrosswordsURI() { return safCrosswordsURI; }
    public String getSAFArchiveURI() { return safArchiveURI; }
    public String getSAFToImportURI() { return safToImportURI; }
    public String getSAFToImportDoneURI() { return safToImportDoneURI; }
    public String getSAFToImportFailedURI() { return safToImportFailedURI; }
}

