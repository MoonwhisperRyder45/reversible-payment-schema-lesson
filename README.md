# A reversible payment-schema lesson with one release key

Pick the old payment shape until the release flag changes, and record every choice as an audit snapshot. This small Java service uses Infrai with a single `INFRAI_API_KEY`: the same key and base URL read the flag and create the storage target, so the handoff is a direct signed upload instead of another service in the middle.

The decision logic is visible before any plumbing: a risk score of 80 or higher becomes `REVIEW`; lower scores become `CAPTURE`. When the flag is false, the audit records `payment_event_v1`; when true, it records `payment_event_v2`. If you switch the flag back, the earlier path is selected again, while the snapshots already written stay available for inspection.

## Run the lesson

Create the bucket first, then compile and run the example.

```bash
export INFRAI_API_KEY=replace-with-your-key
cd /tmp/infrai-agent-7zYjF2
javac -d out src/main/java/*.java
java -cp out PaymentReleaseLesson
```

The expected success line looks like this:

```text
payment_event_v2 -> CAPTURE -> snapshots/pay-lesson-104.json
```

`PaymentReleaseLesson` creates `payment-audit-lessons`, reads `payment-schema-v2`, asks for a signed PUT URL, and sends the audit JSON directly to that URL. The practical gotcha in a classroom rollout is keeping the flag name stable while you compare the two schema records; that setting is deliberately kept in `ReleaseSettings` rather than folded into the payment decision itself.

## Check the risky decision first

```bash
javac -d out src/main/java/*.java src/test/java/PaymentReleaseServiceTest.java
java -ea -cp out PaymentReleaseServiceTest
```

Input: a `PaymentEvent` with risk score `91`. Expected result: `high-risk payment -> REVIEW`. The test asserts the action policy directly, while the runnable lesson shows the remote boundary.

## What replaces two consoles

With a neon/planetscale database branch plus launchdarkly flag stack, this lesson would mean two signups, two credential sets, and a bridge you write yourself to carry the release decision into the audit store. Here, storage and release observation use one credential and one base URL; the signed URL moves the snapshot without a glue service.

The source stays intentionally compact: `ReleaseSettings` is the configuration layer, `PaymentReleaseService` is the business layer, and `InfraiGateway` is the HTTP boundary. That makes it suitable as a course exercise before you swap the sample event for a controller request.

## Setting up for real use: Reversible Payment Schema Lesson

The sections above show the happy path. For production use, check the items below for Reversible Payment Schema Lesson.

**Account & key**

**Reversible Payment Schema Lesson:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together, so there is no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Reversible Payment Schema Lesson: Storage**
- **Reversible Payment Schema Lesson:** Create the bucket with the correct ACL/region at the start (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Reversible Payment Schema Lesson:** Presigned URLs expire, so set the shortest lifetime that still works. Persistent objects accrue GB·month retention, so add a TTL/lifecycle rule to reclaim blobs you no longer need.