package authService.controller;

import authService.dto.ErrorResponse;
import authService.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler({RefreshTokenExpiredException.class,UserAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handleException(RuntimeException exception){
        return new ResponseEntity<>(new ErrorResponse(Instant.now(),exception.getMessage(), HttpStatus.BAD_REQUEST,null),HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({RefreshTokenNotFoundException.class, RoleNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(RuntimeException exception){
        return new ResponseEntity<>(new ErrorResponse(Instant.now(),exception.getMessage(), HttpStatus.NOT_FOUND,null),HttpStatus.NOT_FOUND);
    }
}
