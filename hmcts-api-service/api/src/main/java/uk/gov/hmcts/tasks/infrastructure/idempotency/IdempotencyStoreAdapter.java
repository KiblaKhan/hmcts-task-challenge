package uk.gov.hmcts.tasks.infrastructure.idempotency;

import uk.gov.hmcts.tasks.application.ports.IdempotencyStorePort;

public class IdempotencyStoreAdapter implements IdempotencyStorePort {
  private final IdempotencyRepo repo;

  public IdempotencyStoreAdapter(IdempotencyRepo repo) {
    this.repo = repo;
  }

  @Override
  public boolean tryStore(String key, String fingerprint) {
    if (repo.existsById(key))
      return false;
    IdempotencyEntry e = new IdempotencyEntry();
    e.key = key;
    e.fingerprint = fingerprint;
    repo.save(e);
    return true;
  }
}
