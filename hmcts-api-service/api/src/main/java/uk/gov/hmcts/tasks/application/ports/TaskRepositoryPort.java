package uk.gov.hmcts.tasks.application.ports;

import uk.gov.hmcts.tasks.domain.*;
import java.util.*;

public interface TaskRepositoryPort {
  Task save(Task task);

  Optional<Task> findById(TaskId id);

  List<Task> findAll(int page, int pageSize);

  void delete(TaskId id);
}
