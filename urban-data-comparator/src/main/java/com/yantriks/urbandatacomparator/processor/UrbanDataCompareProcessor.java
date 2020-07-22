package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantriks.urbandatacomparator.configuration.CommonFeignConfig;
import com.yantriks.urbandatacomparator.configuration.ReservationClient;
import com.yantriks.urbandatacomparator.model.UrbanCsvData;
import com.yantriks.urbandatacomparator.model.UrbanCsvOutputData;
import com.yantriks.urbandatacomparator.model.responses.ReservationOrderResponse;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingAPIDocumentCreator;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingAPIUtil;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.util.regex.Pattern;

@Slf4j
@Component
public class UrbanDataCompareProcessor implements Processor {

    @Autowired
    ObjectMapper objectMapper;


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

    @Autowired
    ReservationClient reservationClient;

//    @Autowired
//    CommonFeignConfig.FeignErrorDecoder feignErrorDecoder;

    @Override
    public void process(Exchange exchange) throws Exception {
        UrbanCsvData csvData = exchange.getIn().getBody(UrbanCsvData.class);
        log.debug("csvData  " + csvData);
        UrbanCsvOutputData urbanCsvOutputData = new UrbanCsvOutputData();
        boolean isInvAPIFailed = false;
        StringBuilder csvWriteData = new StringBuilder();
        String orderId = csvData.getOrderId();
        String enterpriseCode = csvData.getEnterpriseCode();
        String reservationId = csvData.getReservationId();
        String documentType = csvData.getDocumentType();
        log.debug("UrbanDataCompareProcessor: CSV Data Input\n" + "OrderId : " + orderId
                + " EnterpriseCode : " + enterpriseCode
                + " ReservationId : " + reservationId
                + " Document Type :" + documentType
        );


        if (reservationId.equals("")) {
            log.error("Reservation Id cant be blank");
            yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_DATA_INCORRECT);
            return;
        }

        StringBuilder reservationUrl = getReservationUrl(reservationId);

        log.debug("Reservation Id present hence first check would be getInventoryReservationList URL {}", reservationUrl);

        try {
            Document inDoc = sterlingAPIDocumentCreator.createInDocForGetInvReservation(reservationId);
            Document getInventoryReservationList = sterlingAPIUtil.invokeSterlingAPI(inDoc, UrbanConstants.API_GET_INV_RESERVATION_LIST);
//            Document getInventoryReservationList = YFCDocument.createDocument("Reservation").getDocument();
            //getInventoryReservationList = sterlingGetInvListCall.executeGetInvListApi(reservationId);
            parseSterlingReservationResponse(csvWriteData, orderId, enterpriseCode, reservationId, documentType, reservationUrl, getInventoryReservationList);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception Caught while calling getInventoryReservationList {} , {}",reservationId, e.getMessage());
            log.error("Cause of Exception", e.getCause());
            yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_GET_INV_RESERVATION_FAILED);
            isInvAPIFailed = true;
        }

        log.debug("UrbanDataCompareProcessor: CSV Data to Write : " + csvWriteData.toString());
        exchange.getIn().setBody(csvWriteData.toString());
    }

    private void parseSterlingReservationResponse(StringBuilder csvWriteData, String orderId, String enterpriseCode, String reservationId, String documentType, StringBuilder reservationUrl, Document getInventoryReservationList) throws Exception {
        if (getInventoryReservationList.getDocumentElement().hasChildNodes()) {
            log.debug("UrbanDataCompareProcessor: Reservation exist in Sterling which means order is not created hence needs to be checked against yantriks");
            parseReservationListDocument(csvWriteData, orderId, enterpriseCode, reservationId, reservationUrl, getInventoryReservationList);
        } else {
            log.debug("UrbanDataCompareProcessor: No Reservation found hence will check and call getOrderList ");
            if (isEmptyOrNull(orderId) || isEmptyOrNull(enterpriseCode)) {
                log.error("UrbanDataCompareProcessor: Reservation Id was blank and either order is NA or enterprisecode is NA hence subsequent comparision can't be made");
                log.error("There can be a possibility that soft reservation is expired or Order is created but orderid and enterprisecode is not passed");
                yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, "", "", UrbanConstants.ERR_DATA_INCORRECT);
            } else {
                getOrderDetails(csvWriteData, orderId, enterpriseCode, reservationId, documentType, reservationUrl);

            }
        }
    }

    private void getOrderDetails(StringBuilder csvWriteData, String orderId, String enterpriseCode, String reservationId, String documentType, StringBuilder reservationUrl) throws Exception {
        log.debug("UrbanDataCompareProcessor: Calling getOrderList API of sterling");
        Document getOrderInDoc = sterlingAPIDocumentCreator.createInDocForGetOrderList(enterpriseCode, orderId, documentType);
        Document getOrderListOP = null;
        try {
            getOrderListOP = sterlingAPIUtil.invokeSterlingAPI(getOrderInDoc, SCXmlUtil.createFromString(UrbanConstants.TEMPLATE_GET_ORDER_LIST), UrbanConstants.API_GET_ORDER_LIST);
//                   getOrderListOP = YFCDocument.getDocumentForXMLFile("D:\\getorderListOutput_fix.xml").getDocument();
            log.debug("getOrderListOP " + SCXmlUtil.getString(getOrderListOP));
            //getOrderListOP = sterlingGetOrderListCall.executeGetOLListApi(orderId, enterpriseCode);
            parseOrderDetailResponse(csvWriteData, orderId, enterpriseCode, reservationId, reservationUrl, getOrderListOP);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception while calling Sterling API : " + e.getMessage() + "Cause : " + e.getCause());
            yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_GET_ORDER_LIST_FAILED);
        }

    }

    private void parseOrderDetailResponse(StringBuilder csvWriteData, String orderId, String enterpriseCode, String reservationId, StringBuilder reservationUrl, Document getOrderListOP) throws Exception {
        UrbanCsvOutputData urbanCsvOutputData = new UrbanCsvOutputData();
        if ("0".equals(getOrderListOP.getDocumentElement().getAttribute(UrbanConstants.A_TOTAL_NUM_OF_RECORDS))) {
            log.info("UrbanDataCompareProcessor: No Records found for in get order list");
            yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_NO_ORDER_FOUND);
        } else {
            log.info("UrbanDataCompareProcessor : Received Orders as part of getOrderList Call");
            String reservationResponse = null;
            try {
                ResponseEntity<ReservationOrderResponse> reservationResponse2 = reservationClient.getReservation(reservationId);
//                reservationResponse = reservationResponse2.getBody() != null ? objectMapper.writeValueAsString( reservationResponse2.getBody() ): null;
                processYantriksReservationResponse(reservationResponse2,getOrderListOP,reservationResponse2.getBody(),reservationId);
                log.debug("reservationResponse " + reservationResponse);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Exception Caught while calling get Reservation : " + e.getMessage());
                log.error("Cause : " + e.getCause());
                log.debug("UrbanDataCompareProcessor :: Yantriks API failed with Exception hence will set the CSV response as GET_RESERVATION_FAILED");
                yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_GET_RESERVATION_FAILED);
            }
        }
    }

    private void parseReservationListDocument(StringBuilder csvWriteData, String orderId, String enterpriseCode, String reservationId, StringBuilder reservationUrl, Document getInventoryReservationList) throws Exception {
        String reservationResponse;
        UrbanCsvOutputData urbanCsvOutputData = new UrbanCsvOutputData();
        try {
//            reservationResponse = yantriksUtil.callYantriksGetOrDeleteAPI(reservationUrl.toString(), UrbanConstants.HTTP_METHOD_GET, UrbanConstants.V_PRODUCT_YAS);
            ResponseEntity<ReservationOrderResponse> reservationResponse2 = reservationClient.getReservation(reservationId);
//            reservationResponse = reservationResponse2.getBody() != null ? objectMapper.writeValueAsString(reservationResponse2.getBody()) : null;
            processYantriksReservationResponse(reservationResponse2,getInventoryReservationList,reservationResponse2.getBody(),reservationId);
            yantriksUtil.populateCSVData(csvWriteData, urbanCsvOutputData);

        } catch (Exception e) {
            log.error("UrbanDataCompareProcessor: Yantriks Get Reservation failed");
            log.debug("UrbanDataCompareProcessor :: Yantriks API failed with Exception hence will set the data to write in CSV");
            yantriksUtil.defaultIncorrectDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, UrbanConstants.ERR_GET_RESERVATION_FAILED);
        }

    }

    private StringBuilder getReservationUrl(String reservationId) {
        StringBuilder reservationUrl = new StringBuilder(UrbanConstants.YANTRIKS_GET_RESERVE_URL);
        reservationUrl.append("/");
        reservationUrl.append(UrbanConstants.V_ORGID_URBN);
        reservationUrl.append("/");
        reservationUrl.append(reservationId);
        return reservationUrl;
    }

    private boolean isEmptyOrNull(String str) {
        return str == null || str.trim().isEmpty();
    }

    /***
     *
     * @param responseEntity
     * @param getInventoryReservationList
     * @param reservationResponse
     * @param reservationId
     * @throws Exception
     */
    public void processYantriksReservationResponse(ResponseEntity responseEntity,Document getInventoryReservationList ,ReservationOrderResponse reservationResponse,String reservationId ) throws Exception {

        switch (responseEntity.getStatusCodeValue()){
            case 200 :
                log.debug("UrbanDataCompareProcessor: Comparing both reservation and getInventoryReservationList output, generating report or/and updating the yantriks");
                 urbanToYantriksCompareUpdate.compareReservationsAndUpdate(getInventoryReservationList, reservationResponse, true, reservationId);
                break;

            case 201:
                log.debug("UrbanDataCompareProcessor : POST operation successful , Reservation created in yantriks ");
                break;

            case 204 :
                log.debug("UrbanDataCompareProcessor: Reservation does not exist hence creating a new one from inventory response");
                 urbanToYantriksInvDirectUpdate.directUpdateToYantriks(getInventoryReservationList);
                break;

            default:
                log.debug("Received status code : "+responseEntity.getStatusCodeValue()+" Response received is : "+responseEntity.toString());
                break;
        }
    }
}
