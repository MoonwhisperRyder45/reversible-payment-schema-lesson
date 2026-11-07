import java.time.Instant;

/** The deliberately small event that a learner can trace from a risk decision to an audit record. */
public record PaymentEvent(String paymentId, String learnerId, long amountCents, int riskScore, Instant occurredAt) {
    public PaymentEvent {
        if (paymentId.isBlank() || learnerId.isBlank() || amountCents <= 0) {
            throw new IllegalArgumentException("A payment needs identifiers and a positive amount.");
        }
    }
}
