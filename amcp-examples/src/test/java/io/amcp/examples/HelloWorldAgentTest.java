package io.amcp.examples;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
class HelloWorldAgentTest {

    @Test
    void testStatus() {
        given()
            .when().get("/hello/status")
            .then()
                .statusCode(200)
                .body("running", is(true))
                .body("brokerType", is("memory"))
                .body("agentCount", greaterThan(0));
    }

    @Test
    void testSendHello() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Quarkus"))
            .when().post("/hello/send")
            .then()
                .statusCode(200)
                .body("status", is("success"))
                .body("request", is("Quarkus"))
                .body("response", containsString("Hello, Quarkus"));
    }

    @Test
    void testPing() {
        given()
            .contentType(ContentType.JSON)
            .when().post("/hello/ping")
            .then()
                .statusCode(200)
                .body("status", is("success"))
                .body("pong", is("pong"));
    }
}
