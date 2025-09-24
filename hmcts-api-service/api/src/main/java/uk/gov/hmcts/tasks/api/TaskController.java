package uk.gov.hmcts.tasks.api;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import uk.gov.hmcts.tasks.application.errors.NotFoundException;
import uk.gov.hmcts.tasks.application.usecases.CreateTaskUseCase;
import uk.gov.hmcts.tasks.application.usecases.DeleteTaskUseCase;
import uk.gov.hmcts.tasks.application.usecases.GetTaskUseCase;
import uk.gov.hmcts.tasks.application.usecases.ListTasksUseCase;
import uk.gov.hmcts.tasks.application.usecases.UpdateTaskStatusUseCase;
import uk.gov.hmcts.tasks.domain.Task;
import uk.gov.hmcts.tasks.domain.TaskStatus;

@RestController
@RequestMapping("/tasks")
@Validated
public class TaskController {
  private final CreateTaskUseCase create;
  private final UpdateTaskStatusUseCase update;
  private final ListTasksUseCase list;
  private final GetTaskUseCase get;
  private final DeleteTaskUseCase delete;

  public TaskController(CreateTaskUseCase create, UpdateTaskStatusUseCase update,
      ListTasksUseCase list, GetTaskUseCase get, DeleteTaskUseCase delete) {
    this.create = create;
    this.update = update;
    this.list = list;
    this.get = get;
    this.delete = delete;
  }

  public record CreateRequest(@NotBlank String title, String description, OffsetDateTime dueAt) {
  }

  public record TaskResponse(String id, String title, String description, String status,
      OffsetDateTime dueAt) {
  }

  public record UpdateStatusRequest(@NotNull TaskStatus status) {
  }

  @PostMapping
  public ResponseEntity<TaskResponse> create(@RequestBody @Validated CreateRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {
    Task t = create.execute(request.title(), request.description(), request.dueAt(), idemKey);
    return ResponseEntity.status(HttpStatus.CREATED).header("Location", "/tasks/" + t.id().value())
        .body(toResponse(t));
  }

  @GetMapping("/{id}")
  public TaskResponse get(@PathVariable String id) {
    try {
      return toResponse(get.execute(id));
    } catch (IllegalArgumentException e) {
      throw new NotFoundException("Task '" + id + "' not found");
    }
  }

  @GetMapping
  public List<TaskResponse> list(@RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(name = "page_size", defaultValue = "20") @Min(1) @Max(100) int pageSize,
      @RequestParam(name = "sort", defaultValue = "dueDate") String sort) {
    return list.execute(page, pageSize, sort).stream().map(this::toResponse).toList();
  }

  @PutMapping("/{id}/status")
  public TaskResponse updateStatus(@PathVariable String id,
      @RequestBody @Validated UpdateStatusRequest request) {
    return toResponse(update.execute(id, request.status()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    delete.execute(id);
  }

  private TaskResponse toResponse(Task t) {
    return new TaskResponse(t.id().value(), t.title(), t.description(), t.status().name(),
        t.dueAt());
  }
}
