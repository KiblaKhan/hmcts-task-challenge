package uk.gov.hmcts.tasks.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, String> {
}
