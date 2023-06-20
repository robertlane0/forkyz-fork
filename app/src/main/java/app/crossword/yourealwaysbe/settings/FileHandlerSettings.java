
package app.crossword.yourealwaysbe.settings;

// TODO: convert to record class when possible
public class FileHandlerSettings {
    private StorageLocation storageLocation;
    private String safRootURI;
    private String safCrosswordsURI;
    private String safArchiveURI;

    public FileHandlerSettings(
        StorageLocation storageLocation,
        String safRootURI,
        String safCrosswordsURI,
        String safArchiveURI
    ) {
        this.storageLocation = storageLocation;
        this.safRootURI = safRootURI;
        this.safCrosswordsURI = safCrosswordsURI;
        this.safArchiveURI = safArchiveURI;
    }

    public StorageLocation getStorageLocation() { return storageLocation; }
    public String getSAFRootURI() { return safRootURI; }
    public String getSAFCrosswordsURI() { return safCrosswordsURI; }
    public String getSAFArchiveURI() { return safArchiveURI; }
}


