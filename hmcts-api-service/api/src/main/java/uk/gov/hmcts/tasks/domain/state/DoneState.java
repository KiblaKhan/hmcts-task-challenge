package uk.gov.hmcts.tasks.domain.state;

import uk.gov.hmcts.tasks.domain.TaskStatus;

public final class DoneState implements TaskState {
  public TaskState start() {
    throw new IllegalStateException("Already DONE");
  }

  public TaskState complete() {
    return this;
  }

  public TaskStatus status() {
    return TaskStatus.DONE;
  }
}
