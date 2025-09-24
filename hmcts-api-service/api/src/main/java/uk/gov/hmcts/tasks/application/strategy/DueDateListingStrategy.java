package uk.gov.hmcts.tasks.application.strategy;

import uk.gov.hmcts.tasks.application.ports.TaskRepositoryPort;
import uk.gov.hmcts.tasks.domain.Task;
import java.util.Comparator;
import java.util.List;

public class DueDateListingStrategy implements TaskListingStrategy {
  private final TaskRepositoryPort repo;

  public DueDateListingStrategy(TaskRepositoryPort repo) {
    this.repo = repo;
  }

  public List<Task> list(int page, int pageSize) {
    return repo.findAll(page, pageSize).stream()
        .sorted(Comparator.comparing(Task::dueAt, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  public String name() {
    return "dueDate";
  }
}
