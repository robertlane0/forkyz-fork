package app.crossword.yourealwaysbe.forkyz.versions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Map;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import android.annotation.TargetApi;
import android.os.Build;

import app.crossword.yourealwaysbe.puz.io.StreamUtils;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class KitKatUtil extends JellyBeanMR2Util {
    @Override
    public boolean isAcceptableCharacterResponse(char c) {
        return !Character.isISOControl(c) && !Character.isSurrogate(c);
    }

    @Override
    public boolean isAlphabetic(char c) {
        return Character.isAlphabetic(c);
    }

    @Override
    public InputStream apacheGetInputStream(
        String url, Map<String, String> headers, int timeout
    ) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicRequestBuilder builder = ClassicRequestBuilder.get(url);
            if (headers != null) {
                for (Entry<String, String> e : headers.entrySet()){
                    builder.addHeader(e.getKey(), e.getValue());
                }
            }

            ClassicHttpRequest request = builder.build();
            return client.execute(request, response -> {
                try(InputStream is = response.getEntity().getContent()) {
                    return StreamUtils.copyInputStream(is);
                }
            });
        }
    }
}
