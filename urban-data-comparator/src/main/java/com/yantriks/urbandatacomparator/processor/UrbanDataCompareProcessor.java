package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantriks.urbandatacomparator.model.UrbanCsvData;
import com.yantriks.urbandatacomparator.model.UrbanURI;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetInvListCall;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetOrderListCall;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import com.yantriks.yih.adapter.util.YantriksCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.util.List;

@Slf4j
@Component
public class UrbanDataCompareProcessor implements Processor {

    @Autowired
    SterlingGetInvListCall sterlingGetInvListCall;

    @Autowired
    SterlingGetOrderListCall sterlingGetOrderListCall;

    @Autowired
    YantriksUtil yantriksUtil;

    @Autowired
    UrbanToYantriksInvDirectUpdate urbanToYantriksInvDirectUpdate;

    @Autowired
    UrbanToYantriksOrderDirectUpdate urbanToYantriksOrderDirectUpdate;

    @Override
    public void process(Exchange exchange) throws Exception {
        UrbanCsvData csvData = exchange.getIn().getBody(UrbanCsvData.class);
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
        System.out.println(")))))))))))))"+SCXmlUtil.getString(getInventoryReservationList.getDocumentElement()));
        System.out.println("KKKK"+getInventoryReservationList.getDocumentElement().hasChildNodes());
        if (getInventoryReservationList.getDocumentElement().hasChildNodes()) {
            log.debug("UrbanDataCompareProcessor: Reservation exist in Sterling which means order is not created hence needs to be checked against yantriks");
            //String reservationResponse = yantriksUtil.callYantriksGetOrDeleteAPI(reservationUrl.toString(), UrbanConstants.HTTP_METHOD_GET, UrbanConstants.V_PRODUCT_YAS);
            String reservationResponse = "FAILURE";
            if (UrbanConstants.V_FAILURE.equals(reservationResponse)) {
                log.debug("UrbanDataCompareProcessor: Reservation does not exist hence creating a new one from inventory response");
                urbanToYantriksInvDirectUpdate.directUpdateToYantriks(getInventoryReservationList);
                log.debug("UrbanDataCompareProcessor: directUpdateToInvYantriks : Done");
            } else {
                log.debug("UrbanDataCompareProcessor: Comparing both reservation and getInventoryReservationList output, generating report or/and updating the yantriks");

            }
        } else {
            log.debug("UrbanDataCompareProcessor: No Reservation found hence will check and call getOrderList ");
            if (isEmptyOrNull(orderId) || isEmptyOrNull(enterpriseCode)) {
                log.error("UrbanDataCompareProcessor: Reservation Id was blank and either order is NA or enterprisecode is NA hence subsequent comparision can't be made");
            } else {
                log.debug("UrbanDataCompareProcessor: Calling getOrderList API of sterling");
                String reservationResponse = yantriksUtil.callYantriksGetOrDeleteAPI(reservationUrl.toString(), UrbanConstants.HTTP_METHOD_GET, UrbanConstants.V_PRODUCT_YAS);
                if (UrbanConstants.V_FAILURE.equals(reservationResponse)) {
                    log.debug("UrbanDataCompareProcessor: Yantriks does not have reservation hence based on getOrderList call output updating yantriks");
                    Document getOrderListOP = sterlingGetOrderListCall.executeGetOLListApi(orderId, enterpriseCode);
                    urbanToYantriksOrderDirectUpdate.directUpdateToYantriks(getOrderListOP);
                } else {
                    log.debug("UrbanDataCompareProcessor: Comparing both reservation and getOrderList Output, generating report or/and updating the yantriks");
                }
            }
        }
    }

    private boolean isEmptyOrNull(String str) {
        return str == null || str.trim().isEmpty();
    }
}
