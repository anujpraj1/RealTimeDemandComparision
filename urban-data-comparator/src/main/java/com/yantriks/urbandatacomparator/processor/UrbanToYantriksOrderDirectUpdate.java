package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yantriks.urbandatacomparator.model.*;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import com.yantriks.urbandatacomparator.validation.UrbanPopulateOrderReservationRequest;
import com.yantriks.yih.adapter.util.YantriksConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

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

    public void directUpdateToYantriks(Document inDoc) {
        YantriksReservationRequest yantriksReservationRequest = urbanPopulateOrderReservationRequest.createReservationRequestFromOrderListOP(inDoc);

        if (compareAndGenerate) {
            log.debug("CompareAndGenerate Flag is turned on, Hence writing it into CSV file");
            log.debug("Logging for now instead of writing in CSV :: " + yantriksReservationRequest.toString());
        }
        if (compareAndUpdate) {
            log.debug("CompareAndUpdate Flag is turned on, Hence calling yantriks api to update in Yantriks");
            StringBuilder lineReserveUrl = new StringBuilder(UrbanConstants.YANTRIKS_LINE_RESERVE_URL);
            lineReserveUrl = urbanURI.getReservationUrl(lineReserveUrl, UrbanConstants.SC_GLOBAL, UrbanConstants.TT_RESERVE,
                    true, false, true, false);
            try {
                ObjectMapper jsonObjMapper = new ObjectMapper();
                String httpBody = jsonObjMapper.writeValueAsString(yantriksReservationRequest);
                log.debug("HttpBody :: " + httpBody);
                String response = yantriksUtil.callYantriksAPI(lineReserveUrl.toString(), UrbanConstants.HTTP_METHOD_POST, httpBody, UrbanConstants.V_PRODUCT_YAS);
                if (YantriksConstants.V_FAILURE.equals(response)) {
                    log.debug("UrbanToYantriksOrderDirectUpdate: Yantriks Reservation Call failed with FAILURE response hence will write the request in file");
                    log.debug("UrbanToYantriksOrderDirectUpdate: Writing the request in file");

                }
            } catch (Exception e) {
                log.error("UrbanToYantriksOrderDirectUpdate : Exception caught while creating reservation : " + e.getMessage());
                log.debug("UrbanToYantriksOrderDirectUpdate: Writing the request in file");

            }
        }
    }
}
