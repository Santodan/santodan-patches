package software.santodan.extension.nuviointro;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** One bounded request, without automatic retries or credential-bearing redirects. */
public final class IntroDbClient {
    public static final class Result {
        public final int code;
        public final String error;
        Result(int code, String error) { this.code = code; this.error = error; }
    }
    public interface ConnectionFactory { HttpURLConnection open() throws Exception; }
    public static Result submit(String key, String body, BooleanSupplier cancelled, Consumer<HttpURLConnection> observer) throws Exception {
        return sendResult(() -> (HttpURLConnection)new URL("https://api.introdb.app/submit").openConnection(), key, body, cancelled, observer);
    }
    public static int send(ConnectionFactory factory, String key, String body, BooleanSupplier cancelled,
            Consumer<HttpURLConnection> observer) throws Exception {
        return sendResult(factory, key, body, cancelled, observer).code;
    }
    public static Result sendResult(ConnectionFactory factory, String key, String body, BooleanSupplier cancelled,
            Consumer<HttpURLConnection> observer) throws Exception {
        if (key.isBlank() || key.contains("\r") || key.contains("\n")) throw new IllegalArgumentException("Invalid API key");
        if (cancelled.getAsBoolean()) throw new CancellationException();
        HttpURLConnection c = factory.open();
        try {
            observer.accept(c);
            c.setConnectTimeout(10000); c.setReadTimeout(15000); c.setInstanceFollowRedirects(false);
            c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setRequestProperty("Authorization", "Bearer " + key);
            c.setRequestProperty("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8); c.setFixedLengthStreamingMode(bytes.length);
            if (cancelled.getAsBoolean()) throw new CancellationException();
            try (OutputStream output = c.getOutputStream()) { output.write(bytes); }
            if (cancelled.getAsBoolean()) throw new CancellationException();
            int code = c.getResponseCode();
            String reason = "";
            if (code >= 400) {
                // An unreadable error body must not discard the known HTTP outcome.
                try (InputStream input = c.getErrorStream()) {
                    if (input != null) {
                        ByteArrayOutputStream data = new ByteArrayOutputStream();
                        byte[] buffer = new byte[512];
                        int count;
                        while (data.size() <= 4096 && !cancelled.getAsBoolean() &&
                                (count = input.read(buffer, 0, Math.min(buffer.length, 4097 - data.size()))) != -1) data.write(buffer, 0, count);
                        if (data.size() <= 4096 && !cancelled.getAsBoolean()) {
                            Object error = new org.json.JSONObject(data.toString("UTF-8")).opt("error");
                            if (error instanceof String) reason = ((String)error).replace(key, "[redacted]")
                                .replaceAll("[\\p{Cntrl}]", " ").trim();
                            if (reason.length() > 240) reason = reason.substring(0, 240) + "…";
                        }
                    }
                } catch (Exception ignored) { }
            }
            return new Result(code, reason);
        } finally { observer.accept(null); c.disconnect(); }
    }
}
