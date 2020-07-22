package com.yantriks.urbandatacomparator.configuration;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class FeignRequestException extends Exception {

    private final String body;
    private HttpStatus httpStatus;

    public FeignRequestException(HttpStatus httpStatus, String body) {
        this.httpStatus = this.httpStatus;
        this.body = body;
    }

}
