package uk.gov.hmcts.tasks.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import uk.gov.hmcts.tasks.application.errors.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
public class ApiErrorHandler {

  private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

  private static ResponseEntity<ProblemDetail> respond(ProblemDetail pd) {
    return ResponseEntity.status(pd.getStatus()).contentType(PROBLEM_JSON).body(pd);
  }

  private static ProblemDetail base(HttpStatus status, String title, String detail,
      String instance) {
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setTitle(title);
    pd.setDetail(detail);
    pd.setInstance(URI.create(instance));
    return pd;
  }

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<ProblemDetail> onNotFound(NotFoundException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("Not Found");
    pd.setDetail(ex.getMessage());
    pd.setInstance(URI.create(req.getRequestURI()));
    return ResponseEntity.status(404).contentType(MediaType.valueOf("application/problem+json"))
        .body(pd);
  }

  /* 422 – body validation (@Valid @RequestBody) */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> onMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpServletRequest req) {
    ProblemDetail pd = base(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed",
        "One or more fields are invalid.", req.getRequestURI());
    List<Map<String, String>> errors =
        ex.getBindingResult().getFieldErrors().stream().map(fe -> Map.of("field", fe.getField(),
            "message", Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid"))).toList();
    pd.setProperty("errors", errors);
    return respond(pd);
  }

  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<ProblemDetail> onConflict(IllegalStateException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setTitle("Conflict");
    pd.setDetail(ex.getMessage());
    pd.setInstance(URI.create(req.getRequestURI()));
    return ResponseEntity.status(409).contentType(MediaType.valueOf("application/problem+json"))
        .body(pd);
  }

  /* 400 – query/path errors, malformed JSON, missing params, type mismatches */
  @ExceptionHandler({ConstraintViolationException.class,
      MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
      HttpMessageNotReadableException.class})
  ResponseEntity<ProblemDetail> onBadRequest(Exception ex, HttpServletRequest req) {
    String detail = (ex instanceof HttpMessageNotReadableException) ? "Malformed JSON request."
        : ex.getMessage();
    ProblemDetail pd = base(HttpStatus.BAD_REQUEST, "Bad Request", detail, req.getRequestURI());
    return respond(pd);
  }

  /* 409 – uniqueness/conflict problems */
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ProblemDetail> onConflict(DataIntegrityViolationException ex,
      HttpServletRequest req) {
    ProblemDetail pd = base(HttpStatus.CONFLICT, "Conflict",
        "The request could not be completed due to a data conflict.", req.getRequestURI());
    return respond(pd);
  }

  /* 500 – final safety net */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> onUnhandled(Exception ex, HttpServletRequest req) {
    ProblemDetail pd = base(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
        "An unexpected error occurred.", req.getRequestURI());
    return respond(pd);
  }
}
