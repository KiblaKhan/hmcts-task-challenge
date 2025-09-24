package uk.gov.hmcts.tasks.application.ports;

public interface IdempotencyStorePort {
    boolean tryStore(String key, String fingerprint);
}
