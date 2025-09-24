package uk.gov.hmcts.tasks.infrastructure.idempotency;

import org.springframework.data.repository.CrudRepository;

public interface IdempotencyRepo extends CrudRepository<IdempotencyEntry, String> {
}
