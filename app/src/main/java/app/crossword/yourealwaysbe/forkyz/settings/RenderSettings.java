
package app.crossword.yourealwaysbe.forkyz.settings;

public record RenderSettings(
    boolean displayScratch,
    boolean suppressHintHighlighting,
    DisplaySeparators displaySeparators,
    boolean inferSeparators
) { }
