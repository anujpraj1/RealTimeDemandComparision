package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantriks.urbandatacomparator.configuration.ReservationClient;
import com.yantriks.urbandatacomparator.model.*;
import com.yantriks.urbandatacomparator.model.request.ReservationOrderRequest;
import com.yantriks.urbandatacomparator.model.responses.ReservationProductLocationResponse;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import com.yantriks.urbandatacomparator.validation.UrbanPopulateOrderReservationRequest;
import com.yantriks.yih.adapter.util.YantriksConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
@Slf4j
public class UrbanToYantriksOrderDirectUpdate {

    @Autowired
    ObjectMapper objectMapper;

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
    ReservationClient reservationClient;


    public UrbanCsvOutputData directUpdateToYantriks(Document inDoc) {
        UrbanCsvOutputData urbanCsvOutputData = new UrbanCsvOutputData();

        ReservationOrderRequest reservationOrderRequest = urbanPopulateOrderReservationRequest.createReservationRequestFromOrderListOP(inDoc);
        log.debug("OrderList XML :: " + SCXmlUtil.getString(inDoc));

        Element eleRoot = inDoc.getDocumentElement();
        Element eleOrder = SCXmlUtil.getChildElement(eleRoot, YantriksConstants.ORDER);
        String orderId = eleOrder.getAttribute(YantriksConstants.ORDER_NO);
        String enterpriseCode = eleOrder.getAttribute(YantriksConstants.A_ENTERPRISE_CODE);

        urbanCsvOutputData.setExtnReservationId(reservationOrderRequest.getOrderId());
        urbanCsvOutputData.setOrderId(orderId);
        urbanCsvOutputData.setEnterpriseCode(enterpriseCode);
        if (compareAndGenerate) {
            log.debug("CompareAndGenerate Flag is turned on, Hence writing it into CSV file");
            log.debug("Logging for now instead of writing in CSV For Order :: " + reservationOrderRequest.toString());
            urbanCsvOutputData.setCompareAndGenerate(true);
            urbanCsvOutputData.setReservationStatus(UrbanConstants.RS_MISSING);
        } else {
            if (reservationOrderRequest.getLineReservationDetails().isEmpty()) {
                log.info("UrbanToYantriksOrderDirectUpdate: Line Reservation Details are empty hence not calling Yantriks API");
                urbanCsvOutputData.setMessage("Cancelled Line");
                urbanCsvOutputData.setCompareAndGenerate(true);
//                urbanCsvOutputData.setReservationStatus(UrbanConstants.MSG_NO_UPDATE_REQUIRED);
                urbanCsvOutputData.setReservationStatus("STATUSES_OUT_OF_RESERVATION");
                urbanCsvOutputData.setReservationResponseCode(998);

            } else {
                String transactionType = determineTransactionType(reservationOrderRequest.toString());
                urbanCsvOutputData.setCompareAndGenerate(false);
                log.debug("CompareAndUpdate Flag is turned on, Hence calling yantriks api to update in Yantriks");
//                StringBuilder reserveUrl = new StringBuilder(UrbanConstants.YANTRIKS_RESERVE_URL);
//                reserveUrl = urbanURI.getReservationUrl(reserveUrl, UrbanConstants.SC_GLOBAL, transactionType,
//                        true, false, false, true);
                try {
//                    String httpBody = objectMapper.writeValueAsString(reservationOrderRequest);
//                    log.debug("HttpBody :: " + httpBody);
//                    log.debug("Reserve URL :: " + reserveUrl);
//                    HttpResponseImpl reservationResponse = yantriksUtil.callYantriksAPI(reserveUrl.toString(), UrbanConstants.HTTP_METHOD_POST, httpBody, UrbanConstants.V_PRODUCT_YAS);
                    ResponseEntity<ReservationProductLocationResponse> response = reservationClient.createReservation(UrbanConstants.SC_GLOBAL, transactionType, reservationOrderRequest, true, false, false, true);
                    log.debug("UrbanToYantriksOrderDirectUpdate: Writing the request in file");

                    urbanCsvOutputData.setReservationResponseCode(response.getStatusCodeValue());
                    urbanCsvOutputData.setError(null);
                    urbanCsvOutputData.setMessage(response.getStatusCode().getReasonPhrase());
                } catch (Exception e) {
                    YantriksUtil.updateOutputDataWithException(urbanCsvOutputData, e);

                }

            }
        }
        return urbanCsvOutputData;
    }

    /***
     *
     * @param yantriksInRequest
     * @return
     */
    private String determineTransactionType(String yantriksInRequest) {

        log.debug("eleOrder " + (yantriksInRequest.toString()));
        if (yantriksInRequest.toString().contains("SCHEDULE_TO")) {
            log.debug(UrbanConstants.TT_TRANSFER);
            return UrbanConstants.TT_TRANSFER;
        } else {
            return UrbanConstants.TT_RESERVE;
        }
    }
}
