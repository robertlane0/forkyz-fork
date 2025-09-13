
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import app.crossword.yourealwaysbe.forkyz.util.NetUtilsKt;

/**
 * Base class for downloaders
 *
 * Provides some useful methods. Recommended to always use
 * getInputStream. If not, set a timeout of getTimeout().
 */
public abstract class AbstractDownloader implements Downloader {
    private int timeoutMillis = 30000;

    public void setTimeout(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public int getTimeout() {
        return timeoutMillis;
    }

    protected BufferedInputStream getInputStream(
        URL url, Map<String, String> headers
    ) throws IOException, URISyntaxException {
        if (headers != null && headers.containsKey("Origin")) {
            // use apache as it allows setting origin via headers
            return new BufferedInputStream(
                NetUtilsKt.apacheGetURLInputStream(
                    url.toURI(),
                    getTimeout(),
                    headers
                )
            );
        } else {
            // more efficient implementation
            return NetUtilsKt.getURLInputStream(
                url,
                getTimeout(),
                headers
            );
        }
    }
}
