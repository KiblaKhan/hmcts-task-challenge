package uk.gov.hmcts.tasks.domain;

import uk.gov.hmcts.tasks.domain.state.*;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.function.Function;

public final class Task {
  private final TaskId id;
  private final String title;
  private final String description;
  private final TaskStatus status;
  private final OffsetDateTime dueAt;

  public Task(TaskId id, String title, String description, TaskStatus status,
      OffsetDateTime dueAt) {
    if (title == null || title.isBlank())
      throw new IllegalArgumentException("title required");
    this.id = id;
    this.title = title.trim();
    this.description = description == null ? "" : description.trim();
    this.status = status == null ? TaskStatus.OPEN : status;
    this.dueAt = dueAt;
  }

  public static Task createNew(String title, String description, OffsetDateTime dueAt) {
    return new Task(TaskId.newId(), title, description, TaskStatus.OPEN, dueAt);
  }

  private static TaskState asState(TaskStatus s) {
    return switch (s) {
      case OPEN -> new OpenState();
      case IN_PROGRESS -> new InProgressState();
      case DONE -> new DoneState();
    };
  }

  public Task transition(Function<TaskState, TaskState> op) {
    TaskState newState = op.apply(asState(this.status));
    return new Task(id, title, description, newState.status(), dueAt);
  }

  public Task updateStatus(TaskStatus newStatus) {
    if (newStatus == TaskStatus.IN_PROGRESS)
      return transition(TaskState::start);
    if (newStatus == TaskStatus.DONE)
      return transition(TaskState::complete);
    if (newStatus == TaskStatus.OPEN)
      return this;
    throw new IllegalArgumentException("invalid status");
  }

  public TaskId id() {
    return id;
  }

  public String title() {
    return title;
  }

  public String description() {
    return description;
  }

  public TaskStatus status() {
    return status;
  }

  public OffsetDateTime dueAt() {
    return dueAt;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Task t && Objects.equals(id, t.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
