
package app.crossword.yourealwaysbe.forkyz.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Map;

public class NetUtils {
    /**
     * Get a buffered input stream for URL
     *
     * @param url url to open
     * @param headers extra request headers
     * @param timeout the timeout in millis
     * @return a buffered input stream
     * @throws IOException
     */
    public static BufferedInputStream getInputStream(
        URL url, Map<String, String> headers, int timeout
    ) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("Connection", "close");
        conn.setRequestProperty("Accept","*/*");

        if (headers != null) {
            for (Entry<String, String> e : headers.entrySet()){
                conn.setRequestProperty(e.getKey(), e.getValue());
            }
        }

        return new BufferedInputStream(conn.getInputStream());
    }

    public static BufferedInputStream getInputStream(
        String url, Map<String, String> headers, int timeout
    ) throws IOException {
        return getInputStream(new URL(url), headers, timeout);
    }
}
