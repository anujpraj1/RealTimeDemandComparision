package com.yantriks.urbandatacomparator.configuration;

import feign.Request;
import feign.RetryableException;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.Date;

@Getter
@Setter
public class FeignGatewayException extends RetryableException {

    private int status;

    public FeignGatewayException(int status, String message, Request.HttpMethod httpMethod, Throwable cause, Date retryAfter, Request request) {
        super(status, message, httpMethod, cause, retryAfter, request);
        this.status = status;
    }

    public FeignGatewayException(int status, String message, Request.HttpMethod httpMethod, Date retryAfter, Request request) {
        super(status, message, httpMethod, retryAfter, request);
        this.status = status;
    }
}
