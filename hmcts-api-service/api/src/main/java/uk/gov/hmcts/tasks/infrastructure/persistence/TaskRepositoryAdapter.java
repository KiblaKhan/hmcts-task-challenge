package uk.gov.hmcts.tasks.infrastructure.persistence;

import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.tasks.application.ports.TaskRepositoryPort;
import uk.gov.hmcts.tasks.domain.*;

import java.util.List;
import java.util.Optional;

public class TaskRepositoryAdapter implements TaskRepositoryPort {
  private final TaskJpaRepository repo;

  public TaskRepositoryAdapter(TaskJpaRepository repo) {
    this.repo = repo;
  }

  @Override
  public Task save(Task t) {
    TaskJpaEntity e = new TaskJpaEntity();
    e.setId(t.id().value());
    e.setTitle(t.title());
    e.setDescription(t.description());
    e.setStatus(t.status().name());
    e.setDueAt(t.dueAt());
    repo.save(e);
    return t;
  }

  @Override
  public Optional<Task> findById(TaskId id) {
    return repo.findById(id.value()).map(this::toDomain);
  }

  @Override
  public List<Task> findAll(int page, int pageSize) {
    return repo.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)).map(this::toDomain)
        .toList();
  }

  @Override
  public void delete(TaskId id) {
    repo.deleteById(id.value());
  }

  private Task toDomain(TaskJpaEntity e) {
    return new Task(new TaskId(e.getId()), e.getTitle(), e.getDescription(),
        TaskStatus.valueOf(e.getStatus()), e.getDueAt());
  }
}
