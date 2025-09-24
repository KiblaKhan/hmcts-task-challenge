package uk.gov.hmcts.tasks.application.usecases;

import uk.gov.hmcts.tasks.application.errors.NotFoundException;
import uk.gov.hmcts.tasks.application.ports.TaskRepositoryPort;
import uk.gov.hmcts.tasks.domain.Task;
import uk.gov.hmcts.tasks.domain.TaskId;

public class GetTaskUseCase {
  private final TaskRepositoryPort repo;

  public GetTaskUseCase(TaskRepositoryPort repo) {
    this.repo = repo;
  }

  public Task execute(String id) {
    return repo.findById(new TaskId(id))
        .orElseThrow(() -> new NotFoundException("Task '" + id + "' not found"));
  }
}
