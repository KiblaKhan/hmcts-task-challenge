package uk.gov.hmcts.tasks.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskStateTest {
  @Test
  void open_to_inprogress_then_done() {
    Task t = Task.createNew("Title", null, null);
    t = t.updateStatus(TaskStatus.IN_PROGRESS);
    assertEquals(TaskStatus.IN_PROGRESS, t.status());
    t = t.updateStatus(TaskStatus.DONE);
    assertEquals(TaskStatus.DONE, t.status());
  }

  @Test
  void cannot_complete_from_open() {
    Task t = Task.createNew("Title", null, null);

    // If you have a capability method, assert it is NOT allowed:
    // assertFalse(t.canTransitionTo(DONE));

    // Core invariant: attempting the transition throws
    assertThrows(IllegalStateException.class, () -> t.updateStatus(TaskStatus.DONE));
  }

  @Test
  void title_required() {
    assertThrows(IllegalArgumentException.class, () -> Task.createNew("  ", null, null));
  }
}
