package uk.gov.hmcts.tasks.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import uk.gov.hmcts.tasks.application.errors.NotFoundException;

class ApiErrorHandlerTest {

    private final ApiErrorHandler handler = new ApiErrorHandler();

    static class Dummy {
        void m(String field1) {
            if (field1 == null) {
            }
        }
    }

    @Test
    void testOnMethodArgumentNotValid_returnsUnprocessableEntityWithErrors() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/test-uri");

        // Real BindingResult (no mocks)
        Object target = new Object();
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(target, "obj");
        br.addError(new FieldError("obj", "field1", "must not be null"));

        // Real MethodParameter (no nulls)
        MethodParameter param =
                new MethodParameter(Dummy.class.getDeclaredMethod("m", String.class), 0);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, br);

        ResponseEntity<ProblemDetail> response = handler.onMethodArgumentNotValid(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd).isNotNull();
        assertThat(pd.getTitle()).isEqualTo("Validation failed");
        assertThat(pd.getDetail()).isEqualTo("One or more fields are invalid.");
        assertThat(pd.getInstance()).isEqualTo(URI.create("/test-uri"));
        List<?> errors = (List<?>) Objects
                .requireNonNull(pd.getProperties(), "getProperties should not be null")
                .get("errors");
        assertThat(errors).hasSize(1);
    }

    @Test
    void testOnBadRequest_withConstraintViolationException_returnsBadRequest() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/bad-request");
        ConstraintViolationException ex = new ConstraintViolationException("Invalid param", null);

        ResponseEntity<ProblemDetail> response = handler.onBadRequest(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd.getTitle()).isEqualTo("Bad Request");
        assertThat(pd.getDetail()).isEqualTo("Invalid param");
        assertThat(pd.getInstance()).isEqualTo(URI.create("/bad-request"));
    }

    @Test
    void testOnBadRequest_withHttpMessageNotReadableException_returnsBadRequestWithMalformedJson() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/bad-json");
        HttpInputMessage input = mock(HttpInputMessage.class);
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Malformed JSON", null, input);

        ResponseEntity<ProblemDetail> response = handler.onBadRequest(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd.getDetail()).isEqualTo("Malformed JSON request.");
    }

    @Test
    void testOnBadRequest_withMissingServletRequestParameterException_returnsBadRequest() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/missing-param");
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("param", "String");

        ResponseEntity<ProblemDetail> response = handler.onBadRequest(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd.getDetail()).contains("param");
    }

    @Test
    void testOnBadRequest_withMethodArgumentTypeMismatchException_returnsBadRequest() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/type-mismatch");
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getMessage()).thenReturn("Type mismatch");

        ResponseEntity<ProblemDetail> response = handler.onBadRequest(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd.getDetail()).isEqualTo("Type mismatch");
    }

    @Test
    void testOnNotFound_returnsNotFound() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/not-found");
        NotFoundException ex = new NotFoundException("Resource not found");

        ResponseEntity<ProblemDetail> response = handler.onNotFound(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd.getTitle()).isEqualTo("Not Found");
        assertThat(pd.getDetail()).isEqualTo("Resource not found");
        assertThat(pd.getInstance()).isEqualTo(URI.create("/not-found"));
    }

    @Test
    void testOnConflict_returnsConflict() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/conflict");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Conflict");

        ResponseEntity<ProblemDetail> response = handler.onConflict(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd.getTitle()).isEqualTo("Conflict");
        assertThat(pd.getDetail())
                .isEqualTo("The request could not be completed due to a data conflict.");
        assertThat(pd.getInstance()).isEqualTo(URI.create("/conflict"));
    }

    @Test
    void testOnUnhandled_returnsInternalServerError() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/error");
        Exception ex = new Exception("Unexpected");

        ResponseEntity<ProblemDetail> response = handler.onUnhandled(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd.getTitle()).isEqualTo("Internal Server Error");
        assertThat(pd.getDetail()).isEqualTo("An unexpected error occurred.");
        assertThat(pd.getInstance()).isEqualTo(URI.create("/error"));
    }

    @Test
    void testOnNotFound_withNotFoundException_returnsNotFound() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/not-found-ex");
        NotFoundException ex = new NotFoundException("Entity not found");

        ResponseEntity<ProblemDetail> response = handler.onNotFound(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd).isNotNull();
        assertThat(pd.getTitle()).isEqualTo("Not Found");
        assertThat(pd.getDetail()).isEqualTo("Entity not found");
        assertThat(pd.getInstance()).isEqualTo(URI.create("/not-found-ex"));
    }

    @Test
    void testOnNotFound_withNullMessage_returnsNotFoundWithNullDetail() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/null-detail");
        NotFoundException ex = new NotFoundException(null);

        ResponseEntity<ProblemDetail> response = handler.onNotFound(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd).isNotNull();
        assertThat(pd.getTitle()).isEqualTo("Not Found");
        assertThat(pd.getDetail()).isNull();
        assertThat(pd.getInstance()).isEqualTo(URI.create("/null-detail"));
    }

    @Test
    void testOnConflict_withIllegalStateException_returnsConflict() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/illegal-conflict");
        IllegalStateException ex = new IllegalStateException("Illegal state occurred");

        ResponseEntity<ProblemDetail> response = handler.onConflict(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd).isNotNull();
        assertThat(pd.getTitle()).isEqualTo("Conflict");
        assertThat(pd.getDetail()).isEqualTo("Illegal state occurred");
        assertThat(pd.getInstance()).isEqualTo(URI.create("/illegal-conflict"));
    }

    @Test
    void testOnConflict_withIllegalStateException_nullMessage_returnsConflictWithNullDetail() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/illegal-conflict-null");
        IllegalStateException ex = new IllegalStateException((String) null);

        ResponseEntity<ProblemDetail> response = handler.onConflict(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail pd =
                Objects.requireNonNull(response.getBody(), "ProblemDetail body expected");
        assertThat(pd).isNotNull();
        assertThat(pd.getTitle()).isEqualTo("Conflict");
        assertThat(pd.getDetail()).isNull();
        assertThat(pd.getInstance()).isEqualTo(URI.create("/illegal-conflict-null"));
    }
}
