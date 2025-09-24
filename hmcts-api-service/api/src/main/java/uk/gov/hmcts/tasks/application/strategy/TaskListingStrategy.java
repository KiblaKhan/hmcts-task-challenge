package uk.gov.hmcts.tasks.application.strategy;

import uk.gov.hmcts.tasks.domain.Task;
import java.util.List;

public interface TaskListingStrategy {
  List<Task> list(int page, int pageSize);

  String name();
}
