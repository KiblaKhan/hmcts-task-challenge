package uk.gov.hmcts.tasks.application.usecases;

import uk.gov.hmcts.tasks.application.errors.NotFoundException;
import uk.gov.hmcts.tasks.application.ports.TaskRepositoryPort;
import uk.gov.hmcts.tasks.domain.Task;
import uk.gov.hmcts.tasks.domain.TaskId;
import uk.gov.hmcts.tasks.domain.TaskStatus;

public class UpdateTaskStatusUseCase {
  private final TaskRepositoryPort repo;

  public UpdateTaskStatusUseCase(TaskRepositoryPort repo) {
    this.repo = repo;
  }

  public Task execute(String id, TaskStatus newStatus) {
    Task task = repo.findById(new TaskId(id))
        .orElseThrow(() -> new NotFoundException("Task '" + id + "' not found"));
    Task updated = task.updateStatus(newStatus);
    return repo.save(updated);
  }
}
