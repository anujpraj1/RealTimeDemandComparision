package com.yantriks.urbandatacomparator.processor;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantriks.urbandatacomparator.model.UrbanCsvData;
import com.yantriks.urbandatacomparator.model.UrbanCsvOutputData;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingAPIDocumentCreator;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingAPIUtil;
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
    UrbanToYantriksInvDirectUpdate urbanToYantriksInvDirectUpdate;

    @Autowired
    UrbanToYantriksOrderDirectUpdate urbanToYantriksOrderDirectUpdate;

    @Autowired
    UrbanToYantriksCompareUpdate urbanToYantriksCompareUpdate;

    @Autowired
    YantriksUtil yantriksUtil;

    @Autowired
    SterlingAPIDocumentCreator sterlingAPIDocumentCreator;

    @Autowired
    SterlingAPIUtil sterlingAPIUtil;

    @Override
    public void process(Exchange exchange) throws Exception {
        UrbanCsvData csvData = exchange.getIn().getBody(UrbanCsvData.class);
        log.debug("csvData  "+csvData);
        UrbanCsvOutputData urbanCsvOutputData = new UrbanCsvOutputData();
        boolean isInvAPIFailed = false;
        boolean isGetOrderAPIFailed = false;
        StringBuilder csvWriteData = new StringBuilder();
        log.debug("UrbanDataCompareProcessor: CSV Data Input");
        log.debug("OrderId : " + csvData.getOrderId());
        log.debug("EnterpriseCode : " + csvData.getEnterpriseCode());
        log.debug("ReservationId : " + csvData.getReservationId());
        log.debug("Document Type :"+csvData.getDocumentType());
        String orderId = csvData.getOrderId();
        String enterpriseCode = csvData.getEnterpriseCode();
        String reservationId = csvData.getReservationId();
        String documentType = csvData.getDocumentType();

        if (reservationId.equals("")) {
            log.error("Reservation Id cant be blank");
            yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_DATA_INCORRECT);
            return;
        }

        StringBuilder reservationUrl = new StringBuilder(UrbanConstants.YANTRIKS_GET_RESERVE_URL);
        reservationUrl.append("/");
        reservationUrl.append(UrbanConstants.V_ORGID_URBN);
        reservationUrl.append("/");
        reservationUrl.append(reservationId);

        log.debug("UrbanDataCompareProcessor: Reservation Id present hence first check would be getInventoryReservationList");
        Document getInventoryReservationList = null;
        Document inDoc = sterlingAPIDocumentCreator.createInDocForGetInvReservation(reservationId);
        try {
            getInventoryReservationList = sterlingAPIUtil.invokeSterlingAPI(inDoc, UrbanConstants.API_GET_INV_RESERVATION_LIST);
            getInventoryReservationList = YFCDocument.createDocument("Reservation").getDocument();
            //getInventoryReservationList = sterlingGetInvListCall.executeGetInvListApi(reservationId);

        } catch (Exception e) {
            log.error("Exception Caught while calling getInventoryReservationList", e.getMessage());
            log.error("Cause of Exception", e.getCause());
            yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_GET_INV_RESERVATION_FAILED);
            isInvAPIFailed = true;
        }
//        System.out.println("DATA :: "+SCXmlUtil.getString(getInventoryReservationList));
        if (!isInvAPIFailed) {
            if (getInventoryReservationList.getDocumentElement().hasChildNodes()) {
                log.debug("UrbanDataCompareProcessor: Reservation exist in Sterling which means order is not created hence needs to be checked against yantriks");
                String reservationResponse = null;
                try {
                    reservationResponse =yantriksUtil.callYantriksGetOrDeleteAPI(reservationUrl.toString(), UrbanConstants.HTTP_METHOD_GET, UrbanConstants.V_PRODUCT_YAS);
                } catch (Exception e) {
                    log.error("UrbanDataCompareProcessor: Yantriks Get Reservation failed");
                    reservationResponse = UrbanConstants.V_EXC_FAILURE;
                }
                String response = yantriksUtil.determineErrorOrSuccessOnReservationPost(reservationResponse);
                if (UrbanConstants.V_FAILURE.equals(response)) {
                    log.debug("UrbanDataCompareProcessor: Reservation does not exist hence creating a new one from inventory response");
                    urbanCsvOutputData = urbanToYantriksInvDirectUpdate.directUpdateToYantriks(getInventoryReservationList);
                    yantriksUtil.populateCSVData(csvWriteData, urbanCsvOutputData);
                    log.debug("UrbanDataCompareProcessor: directUpdateToInvYantriks : Done");
                } else if (UrbanConstants.V_EXC_FAILURE.equals(response)) {
                    log.debug("UrbanDataCompareProcessor :: Yantriks API failed with Exception hence will set the data to write in CSV");
                    yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_GET_RESERVATION_FAILED);
                } else {
                    log.debug("UrbanDataCompareProcessor: Comparing both reservation and getInventoryReservationList output, generating report or/and updating the yantriks");
                    urbanCsvOutputData = urbanToYantriksCompareUpdate.compareReservationsAndUpdate(getInventoryReservationList, reservationResponse, true,reservationId);
                    yantriksUtil.populateCSVData(csvWriteData, urbanCsvOutputData);
                }
            } else {
                log.debug("UrbanDataCompareProcessor: No Reservation found hence will check and call getOrderList ");
                if (isEmptyOrNull(orderId) || isEmptyOrNull(enterpriseCode)) {
                    log.error("UrbanDataCompareProcessor: Reservation Id was blank and either order is NA or enterprisecode is NA hence subsequent comparision can't be made");
                    log.error("There can be a possibility that soft reservation is expired or Order is created but orderid and enterprisecode is not passed");
                    yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, "", "", UrbanConstants.ERR_DATA_INCORRECT);
                } else {
                    log.debug("UrbanDataCompareProcessor: Calling getOrderList API of sterling");
                    Document getOrderInDoc = sterlingAPIDocumentCreator.createInDocForGetOrderList(enterpriseCode, orderId,documentType);
                    Document getOrderListOP = null;
                    try {
                        getOrderListOP = sterlingAPIUtil.invokeSterlingAPI(getOrderInDoc, SCXmlUtil.createFromString(UrbanConstants.TEMPLATE_GET_ORDER_LIST), UrbanConstants.API_GET_ORDER_LIST);
//                   getOrderListOP = YFCDocument.getDocumentForXMLFile("D:\\getorderListOutput_fix.xml").getDocument();
                       log.debug("getOrderListOP "+SCXmlUtil.getString(getOrderListOP));
                        //getOrderListOP = sterlingGetOrderListCall.executeGetOLListApi(orderId, enterpriseCode);
                    } catch (Exception e) {
                        log.error("Exception while calling Sterling API : " + e.getMessage() + "Cause : " + e.getCause());
                        yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_GET_ORDER_LIST_FAILED);
                        isGetOrderAPIFailed = true;
                    }
                    if (!isGetOrderAPIFailed) {
                        if ("0".equals(getOrderListOP.getDocumentElement().getAttribute(UrbanConstants.A_TOTAL_NUM_OF_RECORDS))) {
                            log.info("UrbanDataCompareProcessor: No Records found for in get order list");
                            yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_NO_ORDER_FOUND);
                        } else {
                            log.info("UrbanDataCompareProcessor : Received Orders as part of getOrderList Call");
                            String reservationResponse = null;
                            try {
//                                reservationResponse = yantriksUtil.getJSONFromFile("D:\\getReservation_fix.json");
                                reservationResponse = yantriksUtil.callYantriksGetOrDeleteAPI(reservationUrl.toString(), UrbanConstants.HTTP_METHOD_GET, UrbanConstants.V_PRODUCT_YAS);
                            log.debug("reservationResponse "+reservationResponse);
                            } catch (Exception e) {
                                log.error("Exception Caught while calling get Reservation : " + e.getMessage());
                                log.error("Cause : " + e.getCause());
                                reservationResponse = UrbanConstants.V_EXC_FAILURE;
                            }
                            String response = yantriksUtil.determineErrorOrSuccessOnReservationPost(reservationResponse);
                            log.debug("Response of determineErrorOrSuccessOnReservationPost : " + response);
                            if (UrbanConstants.V_FAILURE.equals(response)) {
                                log.debug("UrbanDataCompareProcessor: Yantriks does not have reservation hence based on getOrderList call output updating yantriks");
                                urbanCsvOutputData = urbanToYantriksOrderDirectUpdate.directUpdateToYantriks(getOrderListOP);
                                yantriksUtil.populateCSVData(csvWriteData, urbanCsvOutputData);
                            } else if (UrbanConstants.V_EXC_FAILURE.equals(response)) {
                                log.debug("UrbanDataCompareProcessor :: Yantriks API failed with Exception hence will set the CSV response as GET_RESERVATION_FAILED");
                                yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_GET_RESERVATION_FAILED);
                            } else {
                                log.debug("UrbanDataCompareProcessor: Comparing both reservation and getOrderList Output or/and updating the yantriks");
                                urbanCsvOutputData = urbanToYantriksCompareUpdate.compareReservationsAndUpdate(getOrderListOP, reservationResponse, false,reservationId);
                                yantriksUtil.populateCSVData(csvWriteData, urbanCsvOutputData);
                            }
                        }
                    }

                }
            }
        }
        log.debug("UrbanDataCompareProcessor: CSV Data to Write : " + csvWriteData.toString());
        exchange.getIn().setBody(csvWriteData.toString());
    }

    private boolean isEmptyOrNull(String str) {
        return str == null || str.trim().isEmpty();
    }
}
