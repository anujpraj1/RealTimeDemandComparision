package com.yantriks.urbandatacomparator.processor;

import com.yantriks.urbandatacomparator.model.UrbanCsvData;
import com.yantriks.urbandatacomparator.model.UrbanCsvOutputData;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetInvListCall;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetOrderListCall;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Slf4j
@Component
public class UrbanDataCompareProcessor implements Processor {

    @Autowired
    SterlingGetInvListCall sterlingGetInvListCall;

    @Autowired
    SterlingGetOrderListCall sterlingGetOrderListCall;


    @Autowired
    UrbanToYantriksInvDirectUpdate urbanToYantriksInvDirectUpdate;

    @Autowired
    UrbanToYantriksOrderDirectUpdate urbanToYantriksOrderDirectUpdate;

    @Autowired
    UrbanToYantriksCompareUpdate urbanToYantriksCompareUpdate;

    @Autowired
    YantriksUtil yantriksUtil;


    @Override
    public void process(Exchange exchange) throws Exception {
        UrbanCsvData csvData = exchange.getIn().getBody(UrbanCsvData.class);
        UrbanCsvOutputData urbanCsvOutputData = new UrbanCsvOutputData();
        log.debug("UrbanDataCompareProcessor: CSV Data Input");
        log.debug("OrderId : "+csvData.getOrderId());
        log.debug("EnterpriseCode : "+csvData.getEnterpriseCode());
        log.debug("ReservationId : "+csvData.getReservationId());
        String orderId = csvData.getOrderId();
        String enterpriseCode = csvData.getEnterpriseCode();
        String reservationId = csvData.getReservationId();

        StringBuilder reservationUrl = new StringBuilder(UrbanConstants.YANTRIKS_GET_RESERVE_URL);
        reservationUrl.append("/");
        reservationUrl.append(UrbanConstants.V_ORGID_URBN);
        reservationUrl.append("/");
        reservationUrl.append(reservationId);

        log.debug("UrbanDataCompareProcessor: Reservation Id present hence first check would be getInventoryReservationList");
        Document getInventoryReservationList = sterlingGetInvListCall.executeGetInvListApi(reservationId);
        if (getInventoryReservationList.getDocumentElement().hasChildNodes()) {
            log.debug("UrbanDataCompareProcessor: Reservation exist in Sterling which means order is not created hence needs to be checked against yantriks");
            String reservationResponse = null;
            try {
                reservationResponse  = yantriksUtil.callYantriksGetOrDeleteAPI(reservationUrl.toString(), UrbanConstants.HTTP_METHOD_GET, UrbanConstants.V_PRODUCT_YAS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (UrbanConstants.V_FAILURE.equals(reservationResponse)) {
                log.debug("UrbanDataCompareProcessor: Reservation does not exist hence creating a new one from inventory response");
                urbanCsvOutputData = urbanToYantriksInvDirectUpdate.directUpdateToYantriks(getInventoryReservationList);
                log.debug("UrbanDataCompareProcessor: directUpdateToInvYantriks : Done");
            } else {
                log.debug("UrbanDataCompareProcessor: Comparing both reservation and getInventoryReservationList output, generating report or/and updating the yantriks");
                urbanCsvOutputData = urbanToYantriksCompareUpdate.compareReservationsAndUpdate(getInventoryReservationList, reservationResponse, false);
            }
        } else {
            log.debug("UrbanDataCompareProcessor: No Reservation found hence will check and call getOrderList ");
            if (isEmptyOrNull(orderId) || isEmptyOrNull(enterpriseCode)) {
                log.error("UrbanDataCompareProcessor: Reservation Id was blank and either order is NA or enterprisecode is NA hence subsequent comparision can't be made");
                log.error("There can be a possibility that soft reservation is expired or Order is created but orderid and enterprisecode is not passed");
            } else {
                log.debug("UrbanDataCompareProcessor: Calling getOrderList API of sterling");
                Document getOrderListOP = sterlingGetOrderListCall.executeGetOLListApi(orderId, enterpriseCode);
                String reservationResponse = null;
                try {
                    reservationResponse = yantriksUtil.callYantriksGetOrDeleteAPI(reservationUrl.toString(), UrbanConstants.HTTP_METHOD_GET, UrbanConstants.V_PRODUCT_YAS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String response = yantriksUtil.determineErrorOrSuccessOnReservationPost(reservationResponse);
                System.out.println("Response :: "+response);
                if (UrbanConstants.V_FAILURE.equals(response)) {
                    log.debug("UrbanDataCompareProcessor: Yantriks does not have reservation hence based on getOrderList call output updating yantriks");
                    urbanCsvOutputData = urbanToYantriksOrderDirectUpdate.directUpdateToYantriks(getOrderListOP);
                } else {
                    log.debug("UrbanDataCompareProcessor: Comparing both reservation and getOrderList Output, generating report or/and updating the yantriks");
                    urbanCsvOutputData = urbanToYantriksCompareUpdate.compareReservationsAndUpdate(getOrderListOP, reservationResponse, false);
                }
            }
        }
        log.info("UrbanDataCompareProcessor : Setting the output to be written to CSV");
        StringBuilder csvWriteData = new StringBuilder();
        if (urbanCsvOutputData.getExtnReservationId() != null || urbanCsvOutputData.getOrderId() != null) {
            csvWriteData.append(urbanCsvOutputData.getOrderId());
            csvWriteData.append("|");
            csvWriteData.append(urbanCsvOutputData.getEnterpriseCode());
            csvWriteData.append("|");
            csvWriteData.append(urbanCsvOutputData.getExtnReservationId());
            csvWriteData.append("|");
            if (urbanCsvOutputData.isCompareAndGenerate()) {
                csvWriteData.append(urbanCsvOutputData.getReservationStatus());
            } else {
                csvWriteData.append(urbanCsvOutputData.getReservationResponseCode());
                csvWriteData.append("|");
                csvWriteData.append(urbanCsvOutputData.getError());
                csvWriteData.append("|");
                csvWriteData.append(urbanCsvOutputData.getMessage());
            }
        } else {
            csvWriteData.append(reservationId);
            csvWriteData.append("|");
            csvWriteData.append(enterpriseCode);
            csvWriteData.append("|");
            csvWriteData.append(orderId);
            csvWriteData.append("|");
            csvWriteData.append("DATA_INCORRECT");
        }
        exchange.getIn().setBody(csvWriteData.toString());
    }

    private boolean isEmptyOrNull(String str) {
        return str == null || str.trim().isEmpty();
    }
}
