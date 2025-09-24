package uk.gov.hmcts.tasks.application.usecases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.tasks.application.strategy.TaskListingStrategy;
import uk.gov.hmcts.tasks.domain.Task;
import uk.gov.hmcts.tasks.domain.TaskId;
import uk.gov.hmcts.tasks.domain.TaskStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ListTasksUseCaseTest {

  private static TaskListingStrategy stub(String name, int count) {
    return new TaskListingStrategy() {
      @Override
      public List<Task> list(int page, int pageSize) {
        // return a list with 'count' dummy tasks so we can assert which strategy ran
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(
                i -> new Task(new TaskId(name + "-" + i), "t" + i, null, TaskStatus.OPEN, null))
            .toList();
      }

      @Override
      public String name() {
        return name;
      }
    };
  }

  @Test
  @DisplayName("picks strategy by name and defaults to dueDate when unknown")
  void picksByName_andDefaultsToDueDate() {
    var dueDate = stub("dueDate", 1);
    var status = stub("status", 2);

    var uc = new ListTasksUseCase(Map.of(dueDate.name(), dueDate, status.name(), status));

    // explicit pick
    var picked = uc.execute(1, 20, "status");
    assertEquals(2, picked.size(), "Should use 'status' strategy");

    // unknown -> default
    var fallback = uc.execute(1, 20, "unknown");
    assertEquals(1, fallback.size(), "Unknown key should default to 'dueDate'");

    // null -> default
    var nullKey = uc.execute(1, 20, null);
    assertEquals(1, nullKey.size(), "Null key should default to 'dueDate'");

    // blank -> default
    var blankKey = uc.execute(1, 20, "   ");
    assertEquals(1, blankKey.size(), "Blank key should default to 'dueDate'");
  }

  @Test
  @DisplayName("constructor requires the default 'dueDate' strategy")
  void requiresDefaultStrategy() {
    var statusOnly = stub("status", 2);
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> new ListTasksUseCase(Map.of(statusOnly.name(), statusOnly)));
    assertTrue(ex.getMessage().contains("dueDate"),
        "Error message should mention missing default 'dueDate' strategy");
  }

  @Test
  @DisplayName("constructor throws when no strategies available (no default present)")
  void throwsIfNoStrategiesOnConstruction() {
    var ex = assertThrows(IllegalStateException.class, () -> new ListTasksUseCase(Map.of()));
    assertTrue(ex.getMessage().contains("dueDate"),
        "Error message should mention missing default 'dueDate' strategy");
  }
}
