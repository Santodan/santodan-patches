package santodan.patches;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Exercises the public fallback without network access or a Reddit account. */
public final class VerifyRedditPostBatch {
    private static String requestedPath;
    private static int requests, status = 200;
    private static boolean disconnected;
    private static String response = "{\"data\":{\"children\":["
        + "{\"data\":{\"name\":\"t3_one\",\"title\":\"First\",\"subreddit\":\"Example\",\"link_flair_text\":\"News\"}},"
        + "{\"data\":{\"name\":\"t3_two\",\"title\":\"Second\",\"subreddit\":\"Example\",\"link_flair_text\":\"\"}},"
        + "{\"data\":{\"name\":\"t3_unrequested\",\"link_flair_text\":\"Ignore\"}}]}}";

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String value(Object post, String name) throws Exception {
        Field field = post.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(post);
    }

    public static void main(String[] args) throws Exception {
        URL.setURLStreamHandlerFactory(protocol -> "https".equals(protocol) ? new URLStreamHandler() {
            @Override protected URLConnection openConnection(URL url) {
                requests++;
                requestedPath = url.getPath();
                disconnected = false;
                return new HttpURLConnection(url) {
                    public void connect() {}
                    public boolean usingProxy() { return false; }
                    public void disconnect() { disconnected = true; }
                    public int getResponseCode() { return status; }
                    public ByteArrayInputStream getInputStream() {
                        return new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
                    }
                };
            }
        } : null);
        Class<?> runtime = Class.forName("software.santodan.extension.redditfilter.RedditContentFilter");
        Method fetch = runtime.getDeclaredMethod("fetchPublicPostDetails", List.class);
        fetch.setAccessible(true);
        Field postsField = runtime.getDeclaredField("POSTS");
        postsField.setAccessible(true);
        Map<?, ?> posts = (Map<?, ?>) postsField.get(null);
        fetch.invoke(null, List.of("t3_one", "t3_two"));
        require(requests == 1 && requestedPath.equals("/by_id/t3_one,t3_two.json"),
            "Multiple posts must use one public request");
        require(disconnected, "Connection must close after success");
        require(value(posts.get("t3_one"), "flair").equals("News"), "Flair must be retained");
        require(value(posts.get("t3_one"), "community").equals("example"), "Community must be normalized");
        require(posts.get("one") == posts.get("t3_one"), "Both ID forms must share the cached result");
        require(value(posts.get("t3_two"), "flair").isEmpty(), "A post without a flair is valid");
        require(!posts.containsKey("t3_unrequested"), "Unrequested IDs must not enter the cache");
        status = 429;
        fetch.invoke(null, List.of("t3_failed"));
        require(disconnected && !posts.containsKey("t3_failed"), "HTTP failure must close without storing data");
        status = 200;
        response = "invalid JSON";
        fetch.invoke(null, List.of("t3_failed"));
        require(disconnected && !posts.containsKey("t3_failed"), "Malformed JSON must close without storing data");
        System.out.println("PASS: batched post fallback, ID aliases, empty flairs, HTTP and JSON failures");
    }
}
