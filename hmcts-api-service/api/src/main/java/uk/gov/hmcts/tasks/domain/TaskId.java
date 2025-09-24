package uk.gov.hmcts.tasks.domain;

import java.util.UUID;

public record TaskId(String value) {
  public static TaskId newId() {
    return new TaskId(UUID.randomUUID().toString());
  }
}
