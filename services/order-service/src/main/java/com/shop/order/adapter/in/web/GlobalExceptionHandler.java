package com.shop.order.adapter.in.web;

import com.shop.order.domain.exception.CurrencyMismatchException;
import com.shop.order.domain.exception.EmptyOrderException;
import com.shop.order.domain.exception.IllegalOrderStateException;
import com.shop.order.domain.exception.InsufficientStockException;
import com.shop.order.domain.exception.OrderNotFoundException;
import com.shop.order.domain.exception.ProductCatalogUnavailableException;
import com.shop.order.domain.exception.ProductNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({ProductNotFoundException.class, EmptyOrderException.class, CurrencyMismatchException.class})
    ProblemDetail unprocessable(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler({InsufficientStockException.class, IllegalOrderStateException.class})
    ProblemDetail conflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ProductCatalogUnavailableException.class)
    ProblemDetail catalogDown(ProductCatalogUnavailableException ex) {
        log.warn("Product catalog unavailable: {}", ex.getCause() == null ? ex.getMessage() : ex.getCause().toString());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(IdempotencyExecutor.InvalidKeyException.class)
    ProblemDetail invalidKey(IdempotencyExecutor.InvalidKeyException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IdempotencyExecutor.KeyReuseException.class)
    ProblemDetail keyReuse(IdempotencyExecutor.KeyReuseException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(IdempotencyExecutor.RequestInProgressException.class)
    ResponseEntity<ProblemDetail> inProgress(IdempotencyExecutor.RequestInProgressException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header("Retry-After", "1")
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage()));
    }
}
