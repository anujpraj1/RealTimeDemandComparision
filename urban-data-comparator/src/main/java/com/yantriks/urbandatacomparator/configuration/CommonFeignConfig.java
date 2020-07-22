/*
 * Copyright (c) 2018 JCPenney Co. All rights reserved.
 */

package com.yantriks.urbandatacomparator.configuration;


import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.io.Reader;

import feign.Logger;
import feign.Response;
import feign.Response.Body;
import feign.Retryer;
import feign.Retryer.Default;
import feign.codec.ErrorDecoder;

@Configuration
@ImportAutoConfiguration({FeignAutoConfiguration.class})
@EnableFeignClients
class CommonFeignConfig {

 //   @Value("${feign.loglevel:basic}")
    Logger.Level logLevel = Logger.Level.BASIC;

    @Value("${feign.retry.period:100}")
    Integer period;

    @Value("${feign.retry.duration:100}")
    Integer duration;

    @Value("${feign.retry.maxAttempts:2}")
    Integer maxAttempts;

    @Value("${feign.coreService.connecttimeout:500}")
    private int connectTimeoutMillis;

    @Value("${feign.coreService.readtimeout:10000}")
    private int readTimeoutMillis;

    @Bean
    public Request.Options options() {
        return new Request.Options(connectTimeoutMillis, readTimeoutMillis);
    }

    @Bean
    public Retryer defaultRetryer() {
        return new Default(period, duration, maxAttempts);
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return logLevel;
    }

    @Bean
    public HttpMessageConverters httpMessageConverters() {
        return new HttpMessageConverters(new MappingJackson2HttpMessageConverter());
    }

    @Bean
    public FeignErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    private static final class FeignErrorDecoder implements ErrorDecoder {

        @Override
        public Exception decode(String methodKey, Response response) {

            StringBuilder buffer = new StringBuilder();
            int status = response.status();
            Reader responseReader;

            try {

                Body responseBody = response.body();

                if (responseBody != null) {
                    responseReader = responseBody.asReader();
                    char[] arr = new char[8 * 1024];
                    int numCharsRead;
                    while ((numCharsRead = responseReader.read(arr, 0, arr.length)) != -1) {
                        buffer.append(arr, 0, numCharsRead);
                    }
                    responseReader.close();
                }

                return new FeignRequestException(HttpStatus.valueOf(status), buffer.toString());

            } catch (IOException e) {
                return e;
            }

        }

    }

}
