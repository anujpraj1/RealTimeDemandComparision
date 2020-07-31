package com.yantriks.urbandatacomparator.model;

import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import com.yantriks.yih.adapter.util.YantriksConstants;
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

    @Value("${urban.yantriks.availability.host}")
    private String availabilityHost;

    @Value("${urban.yantriks.inventorylite.host}")
    private String invLiteHost;

    @Value("${urban.yantriks.masterdata.host}")
    private String commonHost;

    @Value("${urban.yantriks.availability.port}")
    private String availabilityPort;

    @Value("${urban.yantriks.inventorylite.port}")
    private String invLitePort;

    @Value("${urban.yantriks.masterdata.port}")
    private String masterDataPort;

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
//        int index = availabilityHost.toString().indexOf("/");
//        availabilityHost =  availabilityHost.substring(0,index);
        sb.append("://");
        if (availabilityPort == null || availabilityPort.isEmpty()) {
            sb.append(availabilityHost);
        } else {
            sb.append(availabilityHost);
            sb.append(":");
            sb.append(availabilityPort);
        }
        sb.append(urlSuffix);
        return sb.toString();
    }

    public String getInvLiteURL(String urlSuffix) {
        StringBuilder sb = new StringBuilder();
        sb.append(yantriksProtocol);
        sb.append("://");
        if (invLitePort == null || invLitePort.isEmpty()) {
            sb.append(invLiteHost);
        } else {
            sb.append(invLiteHost);
            sb.append(":");
            sb.append(invLitePort);
        }
        sb.append(urlSuffix);
        return sb.toString();
    }

    public String getCommonURL(String urlSuffix) {
        StringBuilder sb = new StringBuilder();
        sb.append(yantriksProtocol);
        sb.append("://");
        if (masterDataPort == null || masterDataPort.isEmpty()) {
            sb.append(commonHost);
        } else {
            sb.append(masterDataPort);
            sb.append(":");
            sb.append(commonHost);
        }
        sb.append(urlSuffix);
        return sb.toString();
    }


    public StringBuilder getReservationUrl(StringBuilder lineReserveUrl, String sellingChannel, String transactionType,
                                           boolean canReserveAfter, boolean considerCapacity, boolean considerGtin, boolean ignoreAvailabilityCheck) {
        lineReserveUrl.append("/");
        lineReserveUrl.append(sellingChannel);
        lineReserveUrl.append("/");
        lineReserveUrl.append(transactionType);
        lineReserveUrl.append("?");
        lineReserveUrl.append(UrbanConstants.CAN_RESERVE_AFTER);
        lineReserveUrl.append("=");
        lineReserveUrl.append(canReserveAfter);
        lineReserveUrl.append("&");
        lineReserveUrl.append(UrbanConstants.CONSIDER_CAPACITY);
        lineReserveUrl.append("=");
        lineReserveUrl.append(considerCapacity);
        lineReserveUrl.append("&");
        lineReserveUrl.append(UrbanConstants.CONSIDER_GTIN);
        lineReserveUrl.append("=");
        lineReserveUrl.append(considerGtin);
        lineReserveUrl.append("&");
        lineReserveUrl.append(UrbanConstants.IGNORE_AVAILABILITY_CHECK);
        lineReserveUrl.append("=");
        lineReserveUrl.append(ignoreAvailabilityCheck);
        return lineReserveUrl;
    }

    public StringBuilder getReservationURLForCancelReservation(StringBuilder reservationURL ,String strOrgId, String strOrderNo , boolean strRestoreCapacity){

        reservationURL.append("/");
        reservationURL.append(strOrgId);
        reservationURL.append("/");
        reservationURL.append(strOrderNo);
        reservationURL.append("?");
        reservationURL.append(YantriksConstants.QUERY_PARAM_RESTORE_CAPACITY);
        reservationURL.append("=");
        reservationURL.append(strRestoreCapacity);
        return reservationURL;
    }

}
