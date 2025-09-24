package uk.gov.hmcts.tasks.domain.state;

import uk.gov.hmcts.tasks.domain.TaskStatus;

public interface TaskState {
  TaskState start();

  TaskState complete();

  TaskStatus status();
}
