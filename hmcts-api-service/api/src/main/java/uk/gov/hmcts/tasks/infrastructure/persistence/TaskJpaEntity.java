package uk.gov.hmcts.tasks.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tasks")
public class TaskJpaEntity {
  @Id
  @Column(name = "id_key", length = 50)
  private String id;
  @Column(nullable = false, length = 255)
  private String title;
  @Column(length = 2000)
  private String description;
  @Column(nullable = false, length = 32)
  private String status;
  @Column(name = "due_at")
  private OffsetDateTime dueAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public OffsetDateTime getDueAt() {
    return dueAt;
  }

  public void setDueAt(OffsetDateTime dueAt) {
    this.dueAt = dueAt;
  }
}
