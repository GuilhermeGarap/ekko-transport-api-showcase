package com.ekko.transport_api.infra.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ValidationErrorDto>> handle400(MethodArgumentNotValidException exception) {
        var errors = exception.getFieldErrors().stream()
                .map(ValidationErrorDto::new)
                .toList();
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<StandardErrorResponse> handleAuthErrors(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "Usuário ou senha inválidos", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", "Acesso negado", request);
    }

    @ExceptionHandler({EntityNotFoundException.class, NotFoundException.class})
    public ResponseEntity<StandardErrorResponse> handle404(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
        ex.printStackTrace();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Erro interno no servidor", request);
    }

    private ResponseEntity<StandardErrorResponse> buildResponse(HttpStatus status, String error, String message, HttpServletRequest request) {
        var response = new StandardErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }

    public record ValidationErrorDto(String field, String message) {
        public ValidationErrorDto(FieldError error) {
            this(error.getField(), error.getDefaultMessage());
        }
    }

    public record StandardErrorResponse(Instant timestamp, int status, String error, String message, String path) {}
}
