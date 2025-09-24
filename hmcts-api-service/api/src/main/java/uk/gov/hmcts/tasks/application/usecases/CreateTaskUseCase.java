package uk.gov.hmcts.tasks.application.usecases;

import uk.gov.hmcts.tasks.application.ports.*;
import uk.gov.hmcts.tasks.domain.Task;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;

public class CreateTaskUseCase {
  private final TaskRepositoryPort repo;
  private final IdempotencyStorePort idem;

  public CreateTaskUseCase(TaskRepositoryPort repo, IdempotencyStorePort idem) {
    this.repo = repo;
    this.idem = idem;
  }

  public Task execute(String title, String description, OffsetDateTime dueAt, String idemKey) {
    if (idemKey != null && !idemKey.isBlank()) {
      String fingerprint = fingerprint(title, description, dueAt);
      boolean ok = idem.tryStore(idemKey, fingerprint);
      if (!ok)
        throw new IllegalStateException("Duplicate request (idempotency)");
    }
    Task t = Task.createNew(title, description, dueAt);
    return repo.save(t);
  }

  // Canonical payload + SHA-256 for a stable, low-collision fingerprint
  private static String fingerprint(String title, String description, OffsetDateTime dueAt) {
    String json = """
        {"method":"POST","path":"/tasks","title":%s,"description":%s,"dueAt":%s}
        """.formatted(quoteOrNull(title), quoteOrNull(description),
        dueAt == null ? "null" : ('"' + dueAt.toString() + '"')).trim();
    return sha256(json.getBytes(StandardCharsets.UTF_8));
  }

  private static String quoteOrNull(String s) {
    if (s == null)
      return "null";
    // very small escape; enough for this context
    return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
  }

  private static String sha256(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(bytes));
    } catch (Exception e) {
      throw new IllegalStateException("Cannot compute fingerprint", e);
    }
  }
}
