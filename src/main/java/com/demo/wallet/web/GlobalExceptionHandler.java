package com.demo.wallet.web;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.demo.wallet.service.InsufficientBalanceException;
import com.demo.wallet.service.InvalidRequestException;
import com.demo.wallet.web.dto.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleInvalid(InvalidRequestException e) {
        return ApiResponse.fail("INVALID_REQUEST", e.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleInsufficient(InsufficientBalanceException e) {
        return ApiResponse.fail("INSUFFICIENT_BALANCE", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleNotFound(NotFoundException e) {
        return ApiResponse.fail("NOT_FOUND", e.getMessage());
    }

    /**
     * HA 데모에서 Failover 순간 DB 연결이 잠깐 끊길 때 503으로 내려주기 위함.
     */
    @ExceptionHandler({
            CannotCreateTransactionException.class,
            TransientDataAccessResourceException.class,
            RecoverableDataAccessException.class,
            DataAccessResourceFailureException.class
    })
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Object> handleDbTransient(Exception e) {
        return ApiResponse.fail("TEMPORARY_DB_UNAVAILABLE", "Temporary DB unavailable. Please retry.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleEtc(Exception e) {
        return ApiResponse.fail("INTERNAL_ERROR", e.getMessage());
    }
}
