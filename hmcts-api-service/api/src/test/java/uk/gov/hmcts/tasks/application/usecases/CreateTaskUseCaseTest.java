package uk.gov.hmcts.tasks.application.usecases;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.tasks.application.ports.IdempotencyStorePort;
import uk.gov.hmcts.tasks.application.ports.TaskRepositoryPort;
import uk.gov.hmcts.tasks.domain.Task;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateTaskUseCaseTest {
  TaskRepositoryPort repo = mock(TaskRepositoryPort.class);
  IdempotencyStorePort idem = mock(IdempotencyStorePort.class);
  CreateTaskUseCase useCase = new CreateTaskUseCase(repo, idem);

  @Test
  void creates_task_and_persists() {
    when(idem.tryStore(any(), any())).thenReturn(true);
    when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    Task t = useCase.execute("Pay fine", "desc", OffsetDateTime.now(), "k1");
    assertEquals("Pay fine", t.title());
    verify(repo).save(any());
  }

  @Test
  void duplicate_idempotency_rejected() {
    when(idem.tryStore(any(), any())).thenReturn(false);
    assertThrows(IllegalStateException.class, () -> useCase.execute("X", null, null, "k1"));
    verify(repo, never()).save(any());
  }
}
