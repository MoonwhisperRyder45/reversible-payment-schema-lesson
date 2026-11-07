import java.time.Instant;

/** Focused business check; run with assertions enabled as shown in the README. */
public final class PaymentReleaseServiceTest {
    public static void main(String[] args) {
        PaymentEvent highRisk = new PaymentEvent("pay-1", "learner-1", 1200, 91, Instant.now());
        String action = PaymentRiskPolicy.actionFor(highRisk.riskScore());
        if (!"REVIEW".equals(action)) throw new AssertionError("High-risk payment must wait for review.");
        System.out.println("high-risk payment -> REVIEW");
    }
}
