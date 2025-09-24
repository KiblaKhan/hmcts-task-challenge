package uk.gov.hmcts.tasks.application.strategy;

import uk.gov.hmcts.tasks.application.ports.TaskRepositoryPort;
import uk.gov.hmcts.tasks.domain.Task;
import uk.gov.hmcts.tasks.domain.TaskStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class StatusListingStrategy implements TaskListingStrategy {
  private final TaskRepositoryPort repo;
  private static final Map<TaskStatus, Integer> ORDER =
      Map.of(TaskStatus.OPEN, 0, TaskStatus.IN_PROGRESS, 1, TaskStatus.DONE, 2);

  public StatusListingStrategy(TaskRepositoryPort repo) {
    this.repo = repo;
  }

  public List<Task> list(int page, int pageSize) {
    return repo.findAll(page, pageSize).stream()
        .sorted(Comparator.comparingInt(t -> ORDER.getOrDefault(t.status(), 99))).toList();
  }

  public String name() {
    return "status";
  }
}
