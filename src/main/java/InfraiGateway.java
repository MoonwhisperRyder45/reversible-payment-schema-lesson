import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small REST adapter shared by storage and release observability through one base URL and one key. */
public final class InfraiGateway {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*true");
    private static final Pattern DEFAULT_VALUE = Pattern.compile("\\\"default_value\\\"\\s*:\\s*(true|false)");
    private static final Pattern URL = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private final HttpClient http = HttpClient.newHttpClient();
    private final ReleaseSettings settings;

    public InfraiGateway(ReleaseSettings settings) {
        this.settings = settings;
    }

    public void ensureAuditBucket() throws Exception {
        call("POST", "/v1/storage/bucket/create", "{\"name\":\"" + settings.auditBucket() + "\"}");
    }

    public boolean flagDefaultValue(String key) throws Exception {
        String json = call("GET", "/v1/flags/get/" + key, null);
        Matcher match = DEFAULT_VALUE.matcher(json);
        if (!match.find()) throw new IllegalStateException("The release flag response has no default value.");
        return Boolean.parseBoolean(match.group(1));
    }

    public String auditUploadUrl(String bucket, String key, String paymentId) throws Exception {
        String body = "{\"op\":\"put\",\"content_type\":\"application/json\",\"max_bytes\":4096,\"idempotency_key\":\"" + paymentId + "\"}";
        String json = call("POST", "/v1/storage/object/presign/" + bucket + "/" + key, body);
        Matcher match = URL.matcher(json);
        if (!match.find()) throw new IllegalStateException("The signed upload response has no URL.");
        return match.group(1).replace("\\/", "/");
    }

    public void putSignedSnapshot(String signedUrl, byte[] snapshot) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(signedUrl))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofByteArray(snapshot))
            .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 500) throw new IllegalStateException("Signed upload transport status.");
        if (response.statusCode() >= 400) throw new IllegalArgumentException("Signed upload result status.");
    }

    private String call(String method, String path, String body) throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(settings.baseUrl() + path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + settings.apiKey())
                .header("Content-Type", "application/json");
            if (body == null) request.method(method, HttpRequest.BodyPublishers.noBody());
            else request.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (OK.matcher(envelope).find()) return envelope;
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep(retryDelay(response, attempt));
                continue;
            }
            if (response.statusCode() >= 500) throw new IllegalStateException("Remote transport status.");
            throw new IllegalArgumentException("Remote request result: " + envelope);
        }
        throw new IllegalStateException("Retry attempts complete.");
    }

    private long retryDelay(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try { return Long.parseLong(retryAfter) * 1000L; }
        catch (NumberFormatException ignored) { return 250L * (1L << attempt); }
    }
}
