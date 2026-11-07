import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Spring-style service boundary: a flag chooses the reversible schema path before a payment is released. */
public final class PaymentReleaseService {
    private final InfraiGateway infrai;
    private final ReleaseSettings settings;

    public PaymentReleaseService(InfraiGateway infrai, ReleaseSettings settings) {
        this.infrai = infrai;
        this.settings = settings;
    }

    public PaymentDecision decide(PaymentEvent event) throws Exception {
        boolean v2Enabled = infrai.flagDefaultValue(settings.schemaFlag());
        String schema = v2Enabled ? "payment_event_v2" : "payment_event_v1";
        String action = PaymentRiskPolicy.actionFor(event.riskScore());
        String notice = action.equals("REVIEW")
            ? "Payment queued for a risk review; the learner is not charged yet."
            : "Payment captured and the enrollment receipt is ready.";

        String audit = "{\"payment_id\":\"" + event.paymentId() + "\",\"learner_id\":\"" + event.learnerId()
            + "\",\"schema\":\"" + schema + "\",\"action\":\"" + action + "\"}";
        String objectKey = "snapshots/" + event.paymentId() + ".json";
        String uploadUrl = infrai.auditUploadUrl(settings.auditBucket(), objectKey, event.paymentId());
        infrai.putSignedSnapshot(uploadUrl, audit.getBytes(StandardCharsets.UTF_8));
        return new PaymentDecision(schema, action, notice, objectKey);
    }

    public record PaymentDecision(String schema, String action, String notification, String auditObjectKey) { }
}
