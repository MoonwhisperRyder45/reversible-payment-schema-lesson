/** Reusable risk rule kept separate from I/O so the release choice can be taught and tested in isolation. */
public final class PaymentRiskPolicy {
    private PaymentRiskPolicy() { }

    public static String actionFor(int riskScore) {
        return riskScore >= 80 ? "REVIEW" : "CAPTURE";
    }
}
