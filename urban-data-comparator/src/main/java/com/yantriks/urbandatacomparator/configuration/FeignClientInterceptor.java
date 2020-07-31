package com.yantriks.urbandatacomparator.configuration;

import com.yantriks.urbandatacomparator.util.GenerateSignedJWTToken;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_TYPE = "Bearer";


    @Value("${security.secretkey}")
    private String strSecretKey;

    @Value("${security.skid}")
    private String strSkid;

    @Value("${security.expireTime}")
    private String strExpirytime;

    @Override
    public void apply(RequestTemplate requestTemplate) {

        String TOKEN = GenerateSignedJWTToken.getJWTTokenStr(strSecretKey, strSkid, strExpirytime);

        requestTemplate.header(AUTHORIZATION_HEADER, String.format("%s %s", TOKEN_TYPE, TOKEN));

    }
}
