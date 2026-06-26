package com.coding.accountservice.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.coding.accountservice.dto.ErrorResponse;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@Autowired
	private Tracer tracer;

	@ExceptionHandler(AccountNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleAccountNotFound(AccountNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream().map(this::formatFieldError)
				.collect(Collectors.joining("; "));
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleBadRequest(HttpMessageNotReadableException ex) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Malformed JSON or invalid enum/date value");
	}

	@ExceptionHandler(IllegalStateException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleBadMetadata(HttpMessageNotReadableException ex) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Failed to serialize metadata");
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ErrorResponse handleGeneric(Exception ex) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
				ex.getMessage() != null ? ex.getMessage() : "Unexpected server error");
	}

	private String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}

	private ErrorResponse build(HttpStatus status, String code, String message) {
		return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), code, message, getTraceId());
	}

	private String getTraceId() {
		Span span = tracer.currentSpan();
		return span != null ? span.context().traceId() : null;
	}
}