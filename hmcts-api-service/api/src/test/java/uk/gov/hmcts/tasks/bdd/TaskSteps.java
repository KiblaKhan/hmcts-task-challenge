package uk.gov.hmcts.tasks.bdd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;

@CucumberContextConfiguration
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TaskSteps {

  @LocalServerPort
  int port;

  @Before
  public void setup() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }

  @When("I create a task titled {string}")
  public void i_create_task(String title) {
    given().header("Content-Type", "application/json").body("{\"title\":\"" + title + "\"}")
        .post("/tasks").then().statusCode(201).body("title", equalTo(title));
  }

  @Then("listing tasks returns at least one task")
  public void list_tasks() {
    given().get("/tasks").then().statusCode(200).body("size()", greaterThan(0));
  }
}
