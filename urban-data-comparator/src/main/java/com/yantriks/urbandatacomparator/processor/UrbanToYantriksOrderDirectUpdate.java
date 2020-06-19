package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantriks.urbandatacomparator.model.*;
import com.yantriks.urbandatacomparator.model.responses.YantriksAvailabilityErrorResponse;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import com.yantriks.urbandatacomparator.validation.UrbanPopulateOrderReservationRequest;
import com.yantriks.yih.adapter.util.YantriksConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
@Slf4j
public class UrbanToYantriksOrderDirectUpdate {

    @Value("${data.mode.comparegenerate}")
    private Boolean compareAndGenerate;

    @Value("${data.mode.compareupdate}")
    private Boolean compareAndUpdate;

    @Autowired
    YantriksUtil yantriksUtil;

    @Autowired
    UrbanPopulateOrderReservationRequest urbanPopulateOrderReservationRequest;

    @Autowired
    UrbanURI urbanURI;

    @Autowired
    UrbanCsvOutputData urbanCsvOutputData;


    public UrbanCsvOutputData directUpdateToYantriks(Document inDoc) {
        YantriksReservationRequest yantriksReservationRequest = urbanPopulateOrderReservationRequest.createReservationRequestFromOrderListOP(inDoc);
        log.debug("OrderList XML :: "+SCXmlUtil.getString(inDoc));
        Element eleRoot = inDoc.getDocumentElement();
        Element eleOrder = SCXmlUtil.getChildElement(eleRoot, YantriksConstants.ORDER);

        if (compareAndGenerate) {
            log.debug("CompareAndGenerate Flag is turned on, Hence writing it into CSV file");
            log.debug("Logging for now instead of writing in CSV For Order :: " + yantriksReservationRequest.toString());
            urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
            urbanCsvOutputData.setOrderId(eleOrder.getAttribute(YantriksConstants.ORDER_NO));
            urbanCsvOutputData.setEnterpriseCode(eleOrder.getAttribute(YantriksConstants.A_ENTERPRISE_CODE));
            urbanCsvOutputData.setCompareAndGenerate(true);
            urbanCsvOutputData.setReservationStatus(UrbanConstants.RS_MISSING);
        } else {
            if (yantriksReservationRequest.getLineReservationDetails().isEmpty()) {
                log.info("UrbanToYantriksOrderDirectUpdate: Line Reservation Details are empty hence not calling Yantriks API");
                urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
                urbanCsvOutputData.setOrderId(eleOrder.getAttribute(YantriksConstants.ORDER_NO));
                urbanCsvOutputData.setEnterpriseCode(eleOrder.getAttribute(YantriksConstants.A_ENTERPRISE_CODE));
                urbanCsvOutputData.setCompareAndGenerate(true);
                urbanCsvOutputData.setReservationStatus("STATUSES_OUT_OF_RESERVATION");
            } else {
                String transactionType = determineTransactionType(yantriksReservationRequest.toString());
                log.debug("CompareAndUpdate Flag is turned on, Hence calling yantriks api to update in Yantriks");
                StringBuilder reserveUrl = new StringBuilder(UrbanConstants.YANTRIKS_RESERVE_URL);
                reserveUrl = urbanURI.getReservationUrl(reserveUrl, UrbanConstants.SC_GLOBAL, transactionType,
                        true, false, false, true);
                try {
                    ObjectMapper jsonObjMapper = new ObjectMapper();
                    String httpBody = jsonObjMapper.writeValueAsString(yantriksReservationRequest);
                    log.debug("HttpBody :: " + httpBody);
                    log.debug("Reserve URL :: "+reserveUrl);
                    String reservationResponse = yantriksUtil.callYantriksAPI(reserveUrl.toString(), UrbanConstants.HTTP_METHOD_POST, httpBody, UrbanConstants.V_PRODUCT_YAS);
                    String response = yantriksUtil.determineErrorOrSuccessOnReservationPost(reservationResponse);
                    if (YantriksConstants.V_FAILURE.equals(response)) {
                        log.debug("UrbanToYantriksOrderDirectUpdate: Yantriks Reservation Call failed with FAILURE response hence will write the request in file");
                        log.debug("UrbanToYantriksOrderDirectUpdate: Writing the request in file");
                        ObjectMapper objOut = new ObjectMapper();
                        YantriksAvailabilityErrorResponse yantriksAvailabilityErrorResponse = objOut.readValue(reservationResponse, YantriksAvailabilityErrorResponse.class);
                        urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
                        urbanCsvOutputData.setOrderId(eleOrder.getAttribute(YantriksConstants.ORDER_NO));
                        urbanCsvOutputData.setEnterpriseCode(eleOrder.getAttribute(YantriksConstants.A_ENTERPRISE_CODE));
                        urbanCsvOutputData.setCompareAndGenerate(false);
                        urbanCsvOutputData.setReservationResponseCode(yantriksAvailabilityErrorResponse.getStatus());
                        urbanCsvOutputData.setError(yantriksAvailabilityErrorResponse.getError());
                        urbanCsvOutputData.setMessage(yantriksAvailabilityErrorResponse.getMessage());
                    } else {
                        urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
                        urbanCsvOutputData.setOrderId(eleOrder.getAttribute(YantriksConstants.ORDER_NO));
                        urbanCsvOutputData.setEnterpriseCode(eleOrder.getAttribute(YantriksConstants.A_ENTERPRISE_CODE));
                        urbanCsvOutputData.setCompareAndGenerate(false);
                        urbanCsvOutputData.setReservationResponseCode(UrbanConstants.RC_201);
                        urbanCsvOutputData.setError("");
                        urbanCsvOutputData.setMessage(UrbanConstants.MSG_SUCCESS);
                    }
                } catch (Exception e) {
                    log.error("UrbanToYantriksOrderDirectUpdate : Exception caught while creating reservation : " + e.getMessage());
                    log.debug("UrbanToYantriksOrderDirectUpdate: Writing the request in file");
                    urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
                    urbanCsvOutputData.setOrderId(eleOrder.getAttribute(YantriksConstants.ORDER_NO));
                    urbanCsvOutputData.setEnterpriseCode(eleOrder.getAttribute(YantriksConstants.A_ENTERPRISE_CODE));
                    urbanCsvOutputData.setCompareAndGenerate(false);
                    urbanCsvOutputData.setReservationResponseCode(500);
                    urbanCsvOutputData.setError(UrbanConstants.ERR_YANT_SERVER_DOWN);
                    urbanCsvOutputData.setMessage("");
                }
            }
        }
        return urbanCsvOutputData;
    }


    private String determineTransactionType(String yantriksInRequest) {

        log.debug("eleOrder "+(yantriksInRequest.toString()));
        if(yantriksInRequest.toString().contains("SCHEDULE_TO")){
            log.debug(UrbanConstants.TT_TRANSFER);
            return UrbanConstants.TT_TRANSFER;
        }
        else{
            return UrbanConstants.TT_RESERVE;
        }
//        String maxOrderStatus = eleOrder.getAttribute(UrbanConstants.A_MAX_ORDER_STATUS);
//        if (UrbanConstants.IM_LIST_ALLOCATED_STATUSES.contains(maxOrderStatus)) {
//            return UrbanConstants.TT_RELEASE;
//        } else if (UrbanConstants.IM_LIST_SCHEDULED_STATUSES.contains(maxOrderStatus)) {
//            return UrbanConstants.TT_SCHEDULE;
//        } else if (UrbanConstants.IM_LIST_OPEN_STATUSES.contains(maxOrderStatus)) {
//            return UrbanConstants.TT_RESERVE;
//        } else if (UrbanConstants.IM_LIST_BACKORDER_STATUSES.contains(maxOrderStatus)) {
//            return UrbanConstants.TT_SCHEDULE;
//        } else {
//            return UrbanConstants.TT_RESERVE;
//        }
    }
}
