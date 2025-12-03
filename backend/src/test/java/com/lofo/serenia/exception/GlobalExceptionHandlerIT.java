package com.lofo.serenia.exception;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.exception.model.ErrorResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for global exception handling.
 * Tests the complete exception handling flow through REST endpoints.
 */
@QuarkusTest
@DisplayName("Global Exception Handler Integration Tests")
@TestProfile(TestResourceProfile.class)
class GlobalExceptionHandlerIT {

    @Test
    @DisplayName("Should return 404 for non-existent endpoint")
    void shouldReturn404ForNonExistentEndpoint() {
        // Act & Assert
        ErrorResponse response = given()
                .when()
                .get("/api/non-existent-endpoint")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract()
                .as(ErrorResponse.class);

        // Assertions
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isNotBlank();
        assertThat(response.traceId()).isNotBlank();
        assertThat(response.id()).isNotBlank();
        assertThat(response.timestamp()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should include trace ID in error response")
    void shouldIncludeTraceIdInErrorResponse() {
        // Act
        ErrorResponse response = given()
                .when()
                .get("/api/non-existent")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponse.class);

        // Assert - Trace ID should be present and not null
        assertThat(response.traceId()).isNotNull();
        assertThat(response.traceId()).isNotBlank();
    }

    @Test
    @DisplayName("Should include request path in error response")
    void shouldIncludeRequestPathInErrorResponse() {
        // Act
        String requestPath = "/api/test-endpoint";
        ErrorResponse response = given()
                .when()
                .get(requestPath)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponse.class);

        // Assert
        assertThat(response.path()).contains(requestPath);
    }

    @Test
    @DisplayName("Error response should contain all required fields")
    void errorResponseShouldContainAllRequiredFields() {
        // Act
        ErrorResponse response = given()
                .when()
                .get("/api/not-found")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponse.class);

        // Assert
        assertThat(response.id()).isNotBlank();
        assertThat(response.timestamp()).isGreaterThan(0);
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isNotBlank();
        assertThat(response.message()).isNotBlank();
        assertThat(response.path()).isNotBlank();
        assertThat(response.traceId()).isNotBlank();
    }

    @Test
    @DisplayName("Different errors should have different trace IDs")
    void differentErrorsShouldHaveDifferentTraceIds() {
        // Act
        String traceId1 = given()
                .when()
                .get("/api/endpoint1")
                .then()
                .extract()
                .as(ErrorResponse.class)
                .traceId();

        String traceId2 = given()
                .when()
                .get("/api/endpoint2")
                .then()
                .extract()
                .as(ErrorResponse.class)
                .traceId();

        // Assert
        assertThat(traceId1).isNotEqualTo(traceId2);
    }

    @Test
    @DisplayName("Error response format should be consistent")
    void errorResponseFormatShouldBeConsistent() {
        // Act
        ErrorResponse response = given()
                .when()
                .get("/api/test")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponse.class);

        // Assert - All fields should follow expected format
        assertThat(response.id()).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertThat(response.timestamp()).isPositive();
        assertThat(response.status()).isPositive();
        assertThat(response.message()).isNotBlank();
    }
}

