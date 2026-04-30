package com.example.tictac.session.config;

import com.example.tictac.session.dto.ErrorResponse;
import com.example.tictac.session.exception.GameEngineCommunicationException;
import com.example.tictac.session.exception.InvalidSessionStateException;
import com.example.tictac.session.exception.SessionNotFoundException;
import com.example.tictac.session.exception.SimulationAlreadyRunningException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Centralised error handling for the Game Session Service.
 *
 * <p>Returns a structured {@link ErrorResponse} body for every error so that
 * clients always receive a consistent shape.
 */
@RestControllerAdvice
public class RestResponseExceptionHandler {

	@ExceptionHandler(SessionNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(SessionNotFoundException ex,
																											HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(SimulationAlreadyRunningException.class)
	public ResponseEntity<ErrorResponse> handleSimulationAlreadyRunning(SimulationAlreadyRunningException ex,
																																			HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidSessionStateException.class)
	public ResponseEntity<ErrorResponse> handleInvalidSessionState(InvalidSessionStateException ex,
																																 HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(GameEngineCommunicationException.class)
	public ResponseEntity<ErrorResponse> handleGameEngineFailure(GameEngineCommunicationException ex,
																															 HttpServletRequest request) {
		return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
																												HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Validation failed");
		return build(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
																																 HttpServletRequest request) {
		String message = ex.getConstraintViolations().stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Constraint violation");
		return build(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler({
			HttpMessageNotReadableException.class,
			MethodArgumentTypeMismatchException.class,
			IllegalArgumentException.class
	})
	public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex,
																												HttpServletRequest request) {
		String message = ex.getMessage() != null ? ex.getMessage() : "Malformed request";
		return build(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex,
																												HttpServletRequest request) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage(), request);
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status,
																							String message,
																							HttpServletRequest request) {
		ErrorResponse body = ErrorResponse.of(
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
}
