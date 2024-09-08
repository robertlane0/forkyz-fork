
package app.crossword.yourealwaysbe.forkyz.settings;

public record FileHandlerSettings(
    StorageLocation storageLocation,
    String safRootURI,
    String safCrosswordsURI,
    String safArchiveURI,
    String safToImportURI,
    String safToImportDoneURI,
    String safToImportFailedURI
) { }

