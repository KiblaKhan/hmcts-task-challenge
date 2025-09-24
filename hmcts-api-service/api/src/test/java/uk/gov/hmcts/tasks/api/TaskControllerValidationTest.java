package uk.gov.hmcts.tasks.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
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

@WebMvcTest(TaskController.class)
class TaskControllerValidationTest {

  @Autowired
  MockMvc mvc;

  @MockBean
  CreateTaskUseCase create;
  @MockBean
  GetTaskUseCase get;
  @MockBean
  UpdateTaskStatusUseCase update;
  @MockBean
  DeleteTaskUseCase delete;
  @MockBean
  ListTasksUseCase list;

  @Test
  void post_empty_title_returns_422_problem() throws Exception {
    mvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(header().string("Content-Type", containsString("application/problem+json")));
  }

  @Test
  void get_type_mismatch_returns_400_problem() throws Exception {
    // id is a path variable; for type mismatch demo, use query params instead (page=abc)
    mvc.perform(get("/tasks?page=abc")).andExpect(status().isBadRequest())
        .andExpect(header().string("Content-Type", containsString("application/problem+json")));
  }
}
