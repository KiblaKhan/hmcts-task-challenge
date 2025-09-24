plugins {
  id("java")
  id("org.springframework.boot") version "3.3.4"
  id("io.spring.dependency-management") version "1.1.6"
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

group = "uk.gov.hmcts.tasks"
version = "0.0.1-SNAPSHOT"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("org.flywaydb:flyway-core")
  implementation("redis.clients:jedis:5.1.2")

  runtimeOnly("com.h2database:h2")
  runtimeOnly("org.postgresql:postgresql")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.mockito:mockito-core")

  // Cucumber (BDD)
  testImplementation("io.cucumber:cucumber-java:7.18.1")
  testImplementation("io.cucumber:cucumber-junit-platform-engine:7.18.1")
  testImplementation("io.cucumber:cucumber-spring:7.18.1")
  testImplementation("org.junit.platform:junit-platform-suite:1.10.3")

  // RestAssured for step defs
  testImplementation("io.rest-assured:rest-assured")

  // Testcontainers for integration tests
  testImplementation("org.testcontainers:postgresql")
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  testLogging {
    events("skipped", "failed")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  }
}

/**
 * Unit tests only by default:
 * - exclude BDD and integration packages from the regular 'test' task
 */
tasks.test {
  exclude("**/uk/gov/hmcts/tasks/bdd/**")
  exclude("**/uk/gov/hmcts/tasks/integration/**")
}

/**
 * Run integration tests (Testcontainers) when RUN_TESTCONTAINERS=true
 * Looks for tests under package: uk.gov.hmcts.tasks.integration
 */
val integrationTest = tasks.register<Test>("integrationTest") {
  description = "Runs integration tests (Testcontainers)."
  group = "verification"
  useJUnitPlatform()
  include("**/uk/gov/hmcts/tasks/integration/**")
  onlyIf { System.getenv("RUN_TESTCONTAINERS") == "true" }
  testLogging { events("skipped", "failed") }
}

/**
 * Run BDD (Cucumber) when RUN_BDD=true
 * Looks for tests under package: uk.gov.hmcts.tasks.bdd
 */
val functionalTest = tasks.register<Test>("functionalTest") {
  description = "Runs BDD/functional tests (Cucumber)."
  group = "verification"
  useJUnitPlatform()
  include("**/uk/gov/hmcts/tasks/bdd/**")
  onlyIf { System.getenv("RUN_BDD") == "true" }
  testLogging { events("skipped", "failed") }
}

/** Add to lifecycle if you want a single entry point */
tasks.check {
  dependsOn(integrationTest, functionalTest)
}
