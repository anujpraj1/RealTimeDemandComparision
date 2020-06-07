package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yantriks.urbandatacomparator.model.*;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import com.yantriks.urbandatacomparator.validation.UrbanPopulateInventoryReservationRequest;
import com.yantriks.yih.adapter.util.YantriksConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
@Slf4j
public class UrbanToYantriksInvDirectUpdate {

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
    UrbanPopulateInventoryReservationRequest urbanPopulateInventoryReservationRequest;

    @Autowired
    UrbanCsvOutputData urbanCsvOutputData;

    public UrbanCsvOutputData directUpdateToYantriks(Document inDoc) throws Exception {

        YantriksReservationRequest yantriksReservationRequest = urbanPopulateInventoryReservationRequest.createReservationRequestFromInventoryReservation(inDoc);

        if (compareAndGenerate) {
            log.debug("CompareAndGenerate Flag is turned on, Hence writing it into CSV file");
            log.debug("Logging for now instead of writing in CSV :: " + yantriksReservationRequest.toString());
            urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
            urbanCsvOutputData.setOrderId("");
            urbanCsvOutputData.setEnterpriseCode("");
            urbanCsvOutputData.setCompareAndGenerate(true);
            urbanCsvOutputData.setReservationStatus(UrbanConstants.RS_MISSING);
        } else {
            log.debug("CompareAndUpdate Flag is turned on, Hence calling yantriks api to update in Yantriks");
            StringBuilder reserveUrl = new StringBuilder(UrbanConstants.YANTRIKS_RESERVE_URL);
            reserveUrl = urbanURI.getReservationUrl(reserveUrl, UrbanConstants.SC_GLOBAL, UrbanConstants.TT_RESERVE,
                    true, false, true, true);
            String response = null;
            try {
                ObjectMapper jsonObjMapper = new ObjectMapper();
                String httpBody = jsonObjMapper.writeValueAsString(yantriksReservationRequest);
                log.debug("HttpBody :: " + httpBody);
                response = yantriksUtil.callYantriksAPI(reserveUrl.toString(), UrbanConstants.HTTP_METHOD_POST, httpBody, UrbanConstants.V_PRODUCT_YAS);
                if (YantriksConstants.V_FAILURE.equals(response)) {
                    log.debug("UrbanToYantriksInvDirectUpdate: Yantriks Reservation Call failed with FAILURE response hence will write the request in file");
                    log.debug("UrbanToYantriksInvDirectUpdate: Writing the request in file");
                    urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
                    urbanCsvOutputData.setOrderId("");
                    urbanCsvOutputData.setEnterpriseCode("");
                    urbanCsvOutputData.setCompareAndGenerate(false);
                    urbanCsvOutputData.setReservationResponseCode(0);
                    urbanCsvOutputData.setError("");
                    urbanCsvOutputData.setMessage("");
                } else {
                    urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
                    urbanCsvOutputData.setOrderId("");
                    urbanCsvOutputData.setEnterpriseCode("");
                    urbanCsvOutputData.setCompareAndGenerate(false);
                    urbanCsvOutputData.setReservationResponseCode(UrbanConstants.RC_201);
                    urbanCsvOutputData.setError("");
                    urbanCsvOutputData.setMessage(UrbanConstants.MSG_SUCCESS);
                }
            } catch (Exception e) {
                log.error("UrbanToYantriksInvDirectUpdate : Exception caught while creating reservation : " + e.getMessage());
                log.debug("UrbanToYantriksInvDirectUpdate: Writing the request in file");
                urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
                urbanCsvOutputData.setOrderId("");
                urbanCsvOutputData.setEnterpriseCode("");
                urbanCsvOutputData.setCompareAndGenerate(false);
                urbanCsvOutputData.setReservationResponseCode(500);
                urbanCsvOutputData.setError(UrbanConstants.ERR_YANT_SERVER_DOWN);
                urbanCsvOutputData.setMessage("");
            }
        }
        return urbanCsvOutputData;
    }
}
