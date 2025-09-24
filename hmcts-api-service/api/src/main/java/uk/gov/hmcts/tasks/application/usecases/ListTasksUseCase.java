package uk.gov.hmcts.tasks.application.usecases;

import uk.gov.hmcts.tasks.application.strategy.TaskListingStrategy;
import uk.gov.hmcts.tasks.domain.Task;

import java.util.List;
import java.util.Map;

public class ListTasksUseCase {
  public static final String DEFAULT_SORT = "dueDate"; // aligns with API default

  private final Map<String, TaskListingStrategy> strategies;

  public ListTasksUseCase(Map<String, TaskListingStrategy> strategies) {
    this.strategies = strategies;
    if (strategies == null || !strategies.containsKey(DEFAULT_SORT)) {
      throw new IllegalStateException("Missing TaskListingStrategy: " + DEFAULT_SORT);
    }
  }

  public List<Task> execute(int page, int pageSize, String strategyName) {
    String key = (strategyName == null || strategyName.isBlank()) ? DEFAULT_SORT : strategyName;
    TaskListingStrategy strat = strategies.getOrDefault(key, strategies.get(DEFAULT_SORT));
    return strat.list(page, pageSize);
  }
}


