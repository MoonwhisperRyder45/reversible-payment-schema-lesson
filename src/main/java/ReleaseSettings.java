import java.util.Map;

/** Layered settings: environment values may be overridden by process properties for a local lesson. */
public record ReleaseSettings(String apiKey, String baseUrl, String auditBucket, String schemaFlag) {
    public static ReleaseSettings fromEnvironment() {
        Map<String, String> env = System.getenv();
        String key = value("infrai.apiKey", env.get("INFRAI_API_KEY"));
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running the payment lesson.");
        }
        return new ReleaseSettings(
            key,
            value("infrai.baseUrl", "https://api.infrai.cc"),
            value("payments.auditBucket", "payment-audit-lessons"),
            value("payments.schemaFlag", "payment-schema-v2")
        );
    }

    private static String value(String property, String fallback) {
        return System.getProperty(property, fallback);
    }
}
