package uk.gov.hmcts.tasks.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.tasks.application.errors.NotFoundException;
import uk.gov.hmcts.tasks.application.ports.TaskRepositoryPort;
import uk.gov.hmcts.tasks.domain.Task;
import uk.gov.hmcts.tasks.domain.TaskId;
import uk.gov.hmcts.tasks.domain.TaskStatus;

class UpdateTaskStatusUseCaseTest {
  TaskRepositoryPort repo = mock(TaskRepositoryPort.class);
  UpdateTaskStatusUseCase uc = new UpdateTaskStatusUseCase(repo);

  @Test
  void updates_and_persists() {
    Task existing = Task.createNew("t", null, null);
    when(repo.findById(existing.id())).thenReturn(Optional.of(existing));
    when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Task updated = uc.execute(existing.id().value(), TaskStatus.IN_PROGRESS);
    assertEquals(TaskStatus.IN_PROGRESS, updated.status());
    verify(repo).save(any());
  }

  @Test
  void throws_when_not_found() {
    String id = "missing-123";
    when(repo.findById(new TaskId(id))).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> uc.execute(id, TaskStatus.OPEN));
    verify(repo, never()).save(any());
  }

  @Test
  void illegal_transition_does_not_persist() {
    // starts in OPEN
    Task existing = Task.createNew("t", null, null);
    when(repo.findById(existing.id())).thenReturn(Optional.of(existing));

    // OPEN -> DONE should throw per state machine
    assertThrows(IllegalStateException.class,
        () -> uc.execute(existing.id().value(), TaskStatus.DONE));

    verify(repo, never()).save(any());
  }
}
