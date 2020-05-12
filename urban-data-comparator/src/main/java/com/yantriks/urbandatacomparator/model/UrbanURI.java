package com.yantriks.urbandatacomparator.model;

import com.yantriks.urbandatacomparator.util.UrbanConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class UrbanURI {

    @Value("${urban.sterling.protocol}")
    private String sterlingProtocol;

    @Value("${urban.sterling.url}")
    private String sterlingURL;

    @Value("${urban.yantriks.protocol}")
    private String yantriksProtocol;

    @Value("${urban.yantriks.availability.url}")
    private String availabilityURL;

    @Value("${urban.yantriks.inventorylite.url}")
    private String invLiteURL;

    @Value("${urban.yantriks.masterdata.url}")
    private String commonURL;

    public String getSterlingURL() {
        StringBuilder sb = new StringBuilder();
        sb.append(sterlingProtocol);
        sb.append("://");
        sb.append(sterlingURL);
        sb.append(UrbanConstants.CONST_API_TESTER_URL);
        return sb.toString();
    }

    public String getAvailabilityURL(String urlSuffix) {
        StringBuilder sb = new StringBuilder();
        sb.append(yantriksProtocol);
        sb.append("://");
        sb.append(availabilityURL);
        sb.append(urlSuffix);
        return sb.toString();
    }

    public String getInvLiteURL(String urlSuffix) {
        StringBuilder sb = new StringBuilder();
        sb.append(yantriksProtocol);
        sb.append("://");
        sb.append(invLiteURL);
        sb.append(urlSuffix);
        return sb.toString();
    }

    public String getCommonURL(String urlSuffix) {
        StringBuilder sb = new StringBuilder();
        sb.append(yantriksProtocol);
        sb.append("://");
        sb.append(commonURL);
        sb.append(urlSuffix);
        return sb.toString();
    }
}
