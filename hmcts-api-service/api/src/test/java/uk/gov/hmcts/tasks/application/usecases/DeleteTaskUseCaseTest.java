package uk.gov.hmcts.tasks.application.usecases;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.tasks.application.errors.NotFoundException;
import uk.gov.hmcts.tasks.application.ports.TaskRepositoryPort;
import uk.gov.hmcts.tasks.domain.*;

class DeleteTaskUseCaseTest {

    @Test
    void shouldDeleteTaskWithGivenId() {
        TaskRepositoryPort repo = mock(TaskRepositoryPort.class);
        DeleteTaskUseCase useCase = new DeleteTaskUseCase(repo);
        String taskId = "123";

        // Arrange: make the task "exist"
        when(repo.findById(new TaskId(taskId)))
                .thenReturn(Optional.of(Task.createNew("t", null, null)));

        // Act
        useCase.execute(taskId);

        // Assert: delete was called with the right id
        verify(repo, times(1)).delete(new TaskId(taskId));
    }

    @Test
    void shouldThrowNotFoundWhenTaskMissing() {
        TaskRepositoryPort repo = mock(TaskRepositoryPort.class);
        DeleteTaskUseCase useCase = new DeleteTaskUseCase(repo);
        String taskId = "missing-123";

        when(repo.findById(new TaskId(taskId))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(taskId));
        verify(repo, never()).delete(any());
    }
}
