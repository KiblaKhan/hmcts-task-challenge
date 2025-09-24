package uk.gov.hmcts.tasks.domain.state;

import uk.gov.hmcts.tasks.domain.TaskStatus;

public final class InProgressState implements TaskState {
  public TaskState start() {
    return this;
  }

  public TaskState complete() {
    return new DoneState();
  }

  public TaskStatus status() {
    return TaskStatus.IN_PROGRESS;
  }
}
