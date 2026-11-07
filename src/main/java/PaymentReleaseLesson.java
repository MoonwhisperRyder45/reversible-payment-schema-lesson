import java.time.Instant;

/** Runnable entry point for a teacher-led schema-release walkthrough. */
public final class PaymentReleaseLesson {
    public static void main(String[] args) throws Exception {
        ReleaseSettings settings = ReleaseSettings.fromEnvironment();
        InfraiGateway infrai = new InfraiGateway(settings);
        infrai.ensureAuditBucket();
        PaymentReleaseService service = new PaymentReleaseService(infrai, settings);
        PaymentEvent event = new PaymentEvent("pay-lesson-104", "learner-28", 4900, 23, Instant.now());
        PaymentReleaseService.PaymentDecision result = service.decide(event);
        System.out.println(result.schema() + " -> " + result.action() + " -> " + result.auditObjectKey());
    }
}
