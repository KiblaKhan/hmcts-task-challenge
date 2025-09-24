package uk.gov.hmcts.tasks.application.usecases;

import uk.gov.hmcts.tasks.application.errors.NotFoundException;
import uk.gov.hmcts.tasks.application.ports.TaskRepositoryPort;
import uk.gov.hmcts.tasks.domain.TaskId;

public class DeleteTaskUseCase {
  private final TaskRepositoryPort repo;

  public DeleteTaskUseCase(TaskRepositoryPort repo) {
    this.repo = repo;
  }

  public void execute(String id) {
    TaskId tid = new TaskId(id);
    if (repo.findById(tid).isEmpty()) {
      throw new NotFoundException("Task '" + id + "' not found");
    }
    repo.delete(tid);
  }
}

