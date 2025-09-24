package uk.gov.hmcts.tasks.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.tasks.application.ports.TaskRepositoryPort;
import uk.gov.hmcts.tasks.domain.Task;
import uk.gov.hmcts.tasks.domain.TaskId;

@ExtendWith(MockitoExtension.class)
class GetTaskUseCaseTest {

    @Mock
    TaskRepositoryPort repo;

    @InjectMocks
    GetTaskUseCase useCase;

    @Test
    void shouldReturnTaskWhenFound() {
        String id = "123";
        TaskId taskId = new TaskId(id);
        Task expectedTask = mock(Task.class);

        when(repo.findById(taskId)).thenReturn(Optional.of(expectedTask));

        Task result = useCase.execute(id);

        assertSame(expectedTask, result);
        verify(repo).findById(taskId);
    }

    @Test
    void shouldThrowExceptionWhenTaskNotFound() {
        String id = "456";
        TaskId taskId = new TaskId(id);

        when(repo.findById(taskId)).thenReturn(Optional.empty());

        uk.gov.hmcts.tasks.application.errors.NotFoundException ex =
                assertThrows(uk.gov.hmcts.tasks.application.errors.NotFoundException.class,
                        () -> useCase.execute(id));
        assertEquals("Task '" + id + "' not found", ex.getMessage());
        verify(repo).findById(taskId);
    }
}
