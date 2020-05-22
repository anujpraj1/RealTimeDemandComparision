package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yantra.yfs.core.YFSSystem;
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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.text.SimpleDateFormat;
import java.util.*;

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

    public void directUpdateToYantriks(Document inDoc) throws Exception {

        YantriksReservationRequest yantriksReservationRequest = urbanPopulateInventoryReservationRequest.createReservationRequestFromInventoryReservation(inDoc);

        if (compareAndGenerate) {
            log.debug("CompareAndGenerate Flag is turned on, Hence writing it into CSV file");
            log.debug("Logging for now instead of writing in CSV :: "+yantriksReservationRequest.toString());
        }
        if (compareAndUpdate) {
            log.debug("CompareAndUpdate Flag is turned on, Hence calling yantriks api to update in Yantriks");
            StringBuilder lineReserveUrl = new StringBuilder(UrbanConstants.YANTRIKS_LINE_RESERVE_URL);
            lineReserveUrl = urbanURI.getReservationUrl(lineReserveUrl, UrbanConstants.SC_GLOBAL, UrbanConstants.TT_RESERVE,
                    true, false, true, false);
            String response = null;
            try {
                ObjectMapper jsonObjMapper = new ObjectMapper();
                String httpBody = jsonObjMapper.writeValueAsString(yantriksReservationRequest);
                log.debug("HttpBody :: "+httpBody);
                response = yantriksUtil.callYantriksAPI(lineReserveUrl.toString(), UrbanConstants.HTTP_METHOD_POST, httpBody, UrbanConstants.V_PRODUCT_YAS);
                if (YantriksConstants.V_FAILURE.equals(response)) {
                    log.debug("UrbanToYantriksInvDirectUpdate: Yantriks Reservation Call failed with FAILURE response hence will write the request in file");
                    log.debug("UrbanToYantriksInvDirectUpdate: Writing the request in file");

                }
            } catch (Exception e) {
                log.error("UrbanToYantriksInvDirectUpdate : Exception caught while creating reservation : "+e.getMessage());
                log.debug("UrbanToYantriksInvDirectUpdate: Writing the request in file");

            }
        }
    }
}
