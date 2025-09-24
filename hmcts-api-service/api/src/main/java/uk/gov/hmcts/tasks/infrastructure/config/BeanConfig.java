package uk.gov.hmcts.tasks.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.tasks.application.ports.*;
import uk.gov.hmcts.tasks.application.strategy.*;
import uk.gov.hmcts.tasks.application.usecases.*;
import uk.gov.hmcts.tasks.infrastructure.idempotency.*;
import uk.gov.hmcts.tasks.infrastructure.persistence.*;

import java.util.Map;

@Configuration
public class BeanConfig {
  @Bean
  TaskRepositoryPort taskRepositoryPort(TaskJpaRepository repo) {
    return new TaskRepositoryAdapter(repo);
  }

  @Bean
  @ConditionalOnProperty(name = "idempotency.backend", havingValue = "redis")
  IdempotencyStorePort redisIdempotencyStore(
      @Value("${redis.url:redis://localhost:6379}") String redisUrl) {
    return new IdempotencyRedisAdapter(redisUrl);
  }

  @Bean
  @ConditionalOnMissingBean(IdempotencyStorePort.class)
  IdempotencyStorePort dbIdempotencyStore(IdempotencyRepo repo) {
    return new IdempotencyStoreAdapter(repo);
  }

  @Bean
  CreateTaskUseCase createTaskUseCase(TaskRepositoryPort r, IdempotencyStorePort i) {
    return new CreateTaskUseCase(r, i);
  }

  @Bean
  UpdateTaskStatusUseCase updateTaskStatusUseCase(TaskRepositoryPort r) {
    return new UpdateTaskStatusUseCase(r);
  }

  @Bean
  ListTasksUseCase listTasksUseCase(TaskRepositoryPort r) {
    TaskListingStrategy due = new DueDateListingStrategy(r);
    TaskListingStrategy status = new StatusListingStrategy(r);
    return new ListTasksUseCase(Map.of(due.name(), due, status.name(), status));
  }

  @Bean
  GetTaskUseCase getTaskUseCase(TaskRepositoryPort r) {
    return new GetTaskUseCase(r);
  }

  @Bean
  DeleteTaskUseCase deleteTaskUseCase(TaskRepositoryPort r) {
    return new DeleteTaskUseCase(r);
  }
}
