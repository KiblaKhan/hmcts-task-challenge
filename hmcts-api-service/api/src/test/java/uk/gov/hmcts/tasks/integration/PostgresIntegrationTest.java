package uk.gov.hmcts.tasks.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.tasks.infrastructure.persistence.TaskJpaRepository;
import uk.gov.hmcts.tasks.infrastructure.persistence.TaskJpaEntity;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "RUN_TESTCONTAINERS", matches = "true")
class PostgresIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  @DynamicPropertySource
  static void dbProps(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  TaskJpaRepository repo;

  @Test
  void saves_and_reads() {
    TaskJpaEntity e = new TaskJpaEntity();
    e.setId("x");
    e.setTitle("t");
    e.setStatus("OPEN");
    repo.save(e);
    assertThat(repo.findById("x")).isPresent();
  }
}
