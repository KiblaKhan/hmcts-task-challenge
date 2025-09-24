package uk.gov.hmcts.tasks.infrastructure.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_entry")
public class IdempotencyEntry {
    @Id
    @Column(name = "id_key", nullable = false, length = 255)
    public String key;

    @Column(name = "fingerprint", nullable = false, length = 255)
    public String fingerprint;
}
