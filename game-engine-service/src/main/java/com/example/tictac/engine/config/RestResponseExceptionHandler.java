package com.example.tictac.engine.config;

import com.example.tictac.engine.dto.ErrorResponse;
import com.example.tictac.engine.exception.GameNotFoundException;
import com.example.tictac.engine.exception.InvalidMoveException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class RestResponseExceptionHandler {

	/**
	 * Handles {@link GameNotFoundException} (HTTP 404).
	 * <p>Thrown by {@link com.example.tictac.engine.service.GameService#getGame(String)}
	 * when no game exists for the requested ID.
	 */
	@ExceptionHandler(GameNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(GameNotFoundException ex,
																											HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	/**
	 * Handles {@link InvalidMoveException} (HTTP 400).
	 * <p>Thrown by {@link com.example.tictac.engine.service.GameService#applyMove}
	 * when the move violates game rules: the target cell is already occupied,
	 * the game has already finished, or it is not the requesting player's turn.
	 */
	@ExceptionHandler(InvalidMoveException.class)
	public ResponseEntity<ErrorResponse> handleInvalidMove(InvalidMoveException ex,
																												 HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	/**
	 * Handles {@link MethodArgumentNotValidException} (HTTP 400).
	 * <p>Raised by Spring when {@code @Valid} validation on {@link com.example.tictac.engine.dto.MoveRequest}
	 * fails in {@link com.example.tictac.engine.controller.GameController#applyMove},
	 * e.g. a null {@code player} or null {@code position} field.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
																												 HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Validation failed");
		return build(HttpStatus.BAD_REQUEST, message, request);
	}

	/**
	 * Handles {@link ConstraintViolationException} (HTTP 400).
	 * <p>Raised by the Bean Validation runtime when the {@code @PositionConstraint}
	 * on {@link com.example.tictac.engine.dto.MoveRequest#position()} is violated,
	 * i.e. the position value is outside the valid board range (0–8).
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
																																 HttpServletRequest request) {
		String message = ex.getConstraintViolations().stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Constraint violation");
		return build(HttpStatus.BAD_REQUEST, message, request);
	}

	/**
	 * Handles malformed or unreadable requests (HTTP 400).
	 * <ul>
	 *   <li>{@link HttpMessageNotReadableException} – raised by Spring when the request body
	 *       is missing, contains invalid JSON, or has an unrecognisable enum value
	 *       (e.g. an invalid {@code player} value in a move request).</li>
	 *   <li>{@link MethodArgumentTypeMismatchException} – raised when a path variable or
	 *       request parameter cannot be converted to the expected type.</li>
	 *   <li>{@link IllegalArgumentException} – a general-purpose guard for any
	 *       invalid argument passed at runtime.</li>
	 * </ul>
	 */
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

	/**
	 * Catch-all handler for any uncaught exception (HTTP 500).
	 * <p>Acts as a safety net so that unexpected errors (e.g. {@link NullPointerException},
	 * database connectivity failures) never leak a raw stack trace to the client.
	 */
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
