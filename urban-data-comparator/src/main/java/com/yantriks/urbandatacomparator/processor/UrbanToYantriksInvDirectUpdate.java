package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yantriks.urbandatacomparator.configuration.ReservationClient;
import com.yantriks.urbandatacomparator.model.*;
import com.yantriks.urbandatacomparator.model.request.ReservationOrderRequest;
import com.yantriks.urbandatacomparator.model.responses.ReservationProductLocationResponse;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import com.yantriks.urbandatacomparator.validation.UrbanPopulateInventoryReservationRequestGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
@Slf4j
public class UrbanToYantriksInvDirectUpdate {

    @Autowired
    ObjectMapper objectMapper;

    @Value("${data.mode.comparegenerate}")
    private Boolean compareAndGenerate;

    @Value("${data.mode.compareupdate}")
    private Boolean compareAndUpdate;

    @Value("${yantriks.default.fulfillmentservice}")
    private String fulfillmentService;

    @Value("${yantriks.default.orgid}")
    private String orgId;

    @Value("${yantriks.default.segment}")
    private String segment;

    @Autowired
    YantriksUtil yantriksUtil;

    @Autowired
    UrbanURI urbanURI;

    @Autowired
    UrbanPopulateInventoryReservationRequestGenerator urbanPopulateInventoryReservationRequest;

    @Autowired
    ReservationClient reservationClient;

    public UrbanCsvOutputData directUpdateToYantriks(Document inDoc) throws Exception {
        UrbanCsvOutputData urbanCsvOutputData = new UrbanCsvOutputData();

        ReservationOrderRequest reservationOrderRequest = urbanPopulateInventoryReservationRequest.createReservationRequestFromInventoryReservation(inDoc);

        if (compareAndGenerate) {
            log.debug("CompareAndGenerate Flag is turned on, Hence writing it into CSV file");
            log.debug("Logging for now instead of writing in CSV :: " + reservationOrderRequest.toString());
            urbanCsvOutputData.setExtnReservationId(reservationOrderRequest.getOrderId());
            urbanCsvOutputData.setOrderId("");
            urbanCsvOutputData.setEnterpriseCode("");
            urbanCsvOutputData.setCompareAndGenerate(true);
            urbanCsvOutputData.setReservationStatus(UrbanConstants.RS_MISSING);
        } else {
            log.debug("CompareAndUpdate Flag is turned on, Hence calling yantriks api to update in Yantriks");
            StringBuilder reserveUrl = new StringBuilder(UrbanConstants.YANTRIKS_RESERVE_URL);
//            reserveUrl = urbanURI.getReservationUrl(reserveUrl, UrbanConstants.SC_GLOBAL, UrbanConstants.TT_RESERVE,
//                    true, false, true, true);
            try {
                String httpBody = objectMapper.writeValueAsString(reservationOrderRequest);
                log.debug("HttpBody :: " + httpBody);
                // HttpResponseImpl reservationResponse = yantriksUtil.callYantriksAPI(reserveUrl.toString(), UrbanConstants.HTTP_METHOD_POST, httpBody, UrbanConstants.V_PRODUCT_YAS);
                //  String response = yantriksUtil.determineErrorOrSuccessOnReservationPost(reservationResponse);
                ResponseEntity<ReservationProductLocationResponse> response = reservationClient.createReservation(UrbanConstants.SC_GLOBAL, UrbanConstants.TT_RESERVE, reservationOrderRequest, true, false, true, true);
//                urbanDataCompareProcessor.processYantriksReservationResponse(response,inDoc,g,reservationId);
                log.debug("UrbanToYantriksInvDirectUpdate: Writing the request in file");
                //YantriksAvailabilityErrorResponse yantriksAvailabilityErrorResponse = objectMapper.readValue(reservationResponse.getBody(), YantriksAvailabilityErrorResponse.class);
                urbanCsvOutputData.setExtnReservationId(reservationOrderRequest.getOrderId());
                urbanCsvOutputData.setOrderId("");
                urbanCsvOutputData.setEnterpriseCode("");
                urbanCsvOutputData.setCompareAndGenerate(false);
                urbanCsvOutputData.setReservationResponseCode(response.getStatusCodeValue());
                urbanCsvOutputData.setError(null);
                urbanCsvOutputData.setMessage(response.getStatusCode().getReasonPhrase());


            } catch (Exception e) {
                YantriksUtil.updateOutputDataWithException(urbanCsvOutputData, e);
            }
        }
        return urbanCsvOutputData;
    }
}
