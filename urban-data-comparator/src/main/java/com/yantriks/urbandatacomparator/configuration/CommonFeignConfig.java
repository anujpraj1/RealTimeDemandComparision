/*
 * Copyright (c) 2018 JCPenney Co. All rights reserved.
 */

package com.yantriks.urbandatacomparator.configuration;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import feign.Request;
import lombok.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
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

    @Value("${feign.retry.period:60000}")
    Integer period;

    @Value("${feign.retry.duration:120000}")
    Integer duration;

    @Value("${feign.retry.maxAttempts:3}")
    Integer maxAttempts;

    @Value("${feign.coreService.connecttimeout:5000}")
    private int connectTimeoutMillis;

    @Value("${feign.coreService.readtimeout:120000}")
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

    public class FeignErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder errorDecoder = new Default();

        @Override
        public Exception decode(String s, Response response) {

            String message = null;
            Reader reader = null;

            try {
                reader = response.body().asReader();
                String result = CharStreams.toString(reader);

                ObjectMapper mapper = new ObjectMapper();
                mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                ExceptionMessage exceptionMessage = mapper.readValue(result,
                        ExceptionMessage.class);

                message = exceptionMessage.message;

            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                try {

                    if (reader != null)
                        reader.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            switch (response.status()) {
                case 400:
                    return new FeignRequestException( HttpStatus.BAD_REQUEST, message == null ? "BAD_REQUEST" :
                            message);

                case 404:
                    return new FeignRequestException( HttpStatus.NOT_FOUND, message == null ? "File not found" :
                            message);
                case 403:
                    return new FeignRequestException(HttpStatus.FORBIDDEN, message == null ? "Forbidden access" : message);
                case 502:
                    return new FeignRequestException(HttpStatus.BAD_GATEWAY, message == null ? "Bad Gateway" : message);



            }

            return errorDecoder.decode(s, response);
        }


    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ExceptionMessage {

        private String timestamp;
        private int status;
        private String error;
        private Object errorLines;
        private String message;
        private String path;

    }

}


