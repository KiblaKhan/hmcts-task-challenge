package uk.gov.hmcts.tasks.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.tasks.application.usecases.CreateTaskUseCase;
import uk.gov.hmcts.tasks.application.usecases.DeleteTaskUseCase;
import uk.gov.hmcts.tasks.application.usecases.GetTaskUseCase;
import uk.gov.hmcts.tasks.application.usecases.ListTasksUseCase;
import uk.gov.hmcts.tasks.application.usecases.UpdateTaskStatusUseCase;
import uk.gov.hmcts.tasks.domain.Task;
import uk.gov.hmcts.tasks.domain.TaskId;
import uk.gov.hmcts.tasks.domain.TaskStatus;


@WebMvcTest(TaskController.class)
class TaskControllerTest {
  @Autowired
  MockMvc mvc;
  @MockBean
  CreateTaskUseCase create;
  @MockBean
  UpdateTaskStatusUseCase update;
  @MockBean
  ListTasksUseCase list;
  @MockBean
  GetTaskUseCase get;
  @MockBean
  DeleteTaskUseCase delete;

  @Test
  void post_creates_201() throws Exception {
    Task t = Task.createNew("Title", null, null);
    Mockito
        .when(
            create.execute(Mockito.eq("Title"), Mockito.isNull(), Mockito.isNull(), Mockito.any()))
        .thenReturn(t);

    mvc.perform(
        post("/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Title\"}"))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Title"));
  }

  @Test
  void post_creates_open_task() throws Exception {
    Task t = Task.createNew("Title", null, null);
    Mockito
        .when(
            create.execute(Mockito.eq("Title"), Mockito.isNull(), Mockito.isNull(), Mockito.any()))
        .thenReturn(t);

    mvc.perform(
        post("/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Title\"}"))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void get_returns_task() throws Exception {
    Task t = Task.createNew("Title", "Desc", OffsetDateTime.parse("2024-06-01T12:00:00Z"));
    Mockito.when(get.execute("123")).thenReturn(t);

    mvc.perform(get("/tasks/123")).andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Title"))
        .andExpect(jsonPath("$.description").value("Desc"))
        .andExpect(jsonPath("$.dueAt").value("2024-06-01T12:00:00Z"));
  }

  @Test
  void list_returns_tasks_accordingto_dueDate() throws Exception {
    Task t1 = Task.createNew("Title1", "Desc1", null);
    Task t2 = Task.createNew("Title2", "Desc2", null);
    Mockito.when(list.execute(1, 20, "dueDate")).thenReturn(List.of(t1, t2));

    mvc.perform(get("/tasks")).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Title1"))
        .andExpect(jsonPath("$[1].title").value("Title2"));
  }

  @Test
  void list_returns_tasks_accordingto_status() throws Exception {
    Task t1 = Task.createNew("Title1", "Desc1", null);
    Task t2 = Task.createNew("Title2", "Desc2", null).updateStatus(TaskStatus.IN_PROGRESS);;
    Mockito.when(list.execute(1, 20, "status")).thenReturn(List.of(t1, t2));

    mvc.perform(get("/tasks?sort=status")).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Title1"))
        .andExpect(jsonPath("$[0].status").value("OPEN"))
        .andExpect(jsonPath("$[1].title").value("Title2"))
        .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"));
  }

  @Test
  void updateStatus_toInProgress() throws Exception {
    Task t = Task.createNew("Title", null, null).updateStatus(TaskStatus.IN_PROGRESS);
    Mockito.when(update.execute(Mockito.eq("123"), Mockito.eq(TaskStatus.IN_PROGRESS)))
        .thenReturn(t);

    mvc.perform(put("/tasks/123/status").contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"IN_PROGRESS\"}")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  @Test
  void updateStatus_toDone_throwsIllegalStateException() throws Exception {
    Assertions.assertThrows(IllegalStateException.class, () -> {
      Task.createNew("Title", null, null).updateStatus(TaskStatus.DONE);
    });
    // Mockito.when(update.execute(Mockito.eq("123"), Mockito.eq(TaskStatus.DONE)))
    // .thenThrow(new IllegalStateException("Cannot complete from OPEN"));
  }

  @Test
  void delete_removes_task_returns_204() throws Exception {
    Mockito.doNothing().when(delete).execute("123");

    mvc.perform(delete("/tasks/123")).andExpect(status().isNoContent());
  }

  @Test
  void post_returns_Location_header() throws Exception {
    Task t = new Task(new TaskId("abc-123"), "Title", null, TaskStatus.OPEN, null);
    Mockito.when(
        create.execute(Mockito.eq("Title"), Mockito.isNull(), Mockito.isNull(), Mockito.isNull()))
        .thenReturn(t);

    mvc.perform(
        post("/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Title\"}"))
        .andExpect(status().isCreated()).andExpect(header().string("Location", "/tasks/abc-123"));
  }

  @Test
  void duplicate_idempotency_returns_409_problem() throws Exception {
    Mockito
        .when(create.execute(Mockito.eq("Title"), Mockito.isNull(), Mockito.isNull(),
            Mockito.eq("k1")))
        .thenThrow(new IllegalStateException("Duplicate request (idempotency)"));

    mvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON)
        .header("Idempotency-Key", "k1").content("{\"title\":\"Title\"}"))
        .andExpect(status().isConflict()).andExpect(header().string("Content-Type",
            org.hamcrest.Matchers.containsString("application/problem+json")));
  }

  @Test
  void get_missing_returns_404_problem() throws Exception {
    Mockito.when(get.execute(Mockito.eq("missing-1"))).thenThrow(
        new uk.gov.hmcts.tasks.application.errors.NotFoundException("Task 'missing-1' not found"));

    mvc.perform(get("/tasks/missing-1")).andExpect(status().isNotFound()).andExpect(header()
        .string("Content-Type", org.hamcrest.Matchers.containsString("application/problem+json")));
  }

  @Test
  void update_illegal_transition_returns_409_problem() throws Exception {
    Mockito.when(update.execute(Mockito.eq("123"), Mockito.eq(TaskStatus.DONE)))
        .thenThrow(new IllegalStateException("Cannot complete from OPEN"));

    mvc.perform(put("/tasks/123/status").contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"DONE\"}")).andExpect(status().isConflict())
        .andExpect(header().string("Content-Type",
            org.hamcrest.Matchers.containsString("application/problem+json")));
  }

  @Test
  void list_with_invalid_page_returns_400_problem() throws Exception {
    mvc.perform(get("/tasks?page=0&page_size=20&sort=dueDate")).andExpect(status().isBadRequest())
        .andExpect(header().string("Content-Type",
            org.hamcrest.Matchers.containsString("application/problem+json")));
  }

}
