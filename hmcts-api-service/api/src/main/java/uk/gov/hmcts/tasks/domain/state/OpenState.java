package uk.gov.hmcts.tasks.domain.state;

import uk.gov.hmcts.tasks.domain.TaskStatus;

public final class OpenState implements TaskState {
  public TaskState start() {
    return new InProgressState();
  }

  public TaskState complete() {
    throw new IllegalStateException("Cannot complete from OPEN");
  }

  public TaskStatus status() {
    return TaskStatus.OPEN;
  }
}
