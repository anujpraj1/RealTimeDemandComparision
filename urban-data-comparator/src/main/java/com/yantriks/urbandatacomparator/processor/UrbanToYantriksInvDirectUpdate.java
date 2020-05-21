package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yantra.yfs.core.YFSSystem;
import com.yantriks.urbandatacomparator.model.*;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
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

    public void directUpdateToYantriks(Document inDoc) throws Exception {
        Element eleRoot = inDoc.getDocumentElement();
        NodeList nlInvReservations = eleRoot.getElementsByTagName(UrbanConstants.ELE_INV_RESERVATION);
        log.debug("UrbanToYantriksInvDirectUpdate: directUpdateToYantriks: Calculating the ExpirationTime and ExpirationTimeUnit");
        Element firstInvReservation = (Element) nlInvReservations.item(0);
        String expirationDate = firstInvReservation.getAttribute(UrbanConstants.A_EXPIRATION_DATE);
        int expirationTime = 0; //Default
        String expirationTimeUnit = UrbanConstants.V_SECONDS;
        if (expirationDate != null || !expirationDate.isEmpty()) {
            List<String> expirations = yantriksUtil.calculateExpirationTimeAndDate(expirationDate);
            expirationTime = Integer.parseInt(expirations.get(0));
            expirationTimeUnit = expirations.get(1);
        }
        SimpleDateFormat updateTimeFormatter = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

        int counter = 1;
        List<YantriksLineReservationDetailsRequest> lineReservationDetailsRequests = new ArrayList<>();
        for (int i = 0; i< nlInvReservations.getLength(); i++) {
            Element currInvReservation = (Element) nlInvReservations.item(i);
            String qty = currInvReservation.getAttribute(UrbanConstants.A_QUANTITY);
            int qtyToPut = (int) Double.parseDouble(qty);

            List<YantriksReservationDemandTypeRequest> reservationDemandTypeRequests = new ArrayList<>();
            YantriksReservationDemandTypeRequest yantriksReservationDemandTypeRequest = YantriksReservationDemandTypeRequest.builder()
                    .demandType(UrbanConstants.DT_RESERVED)
                    .quantity(qtyToPut)
                    .reservationDate(currInvReservation.getAttribute(UrbanConstants.A_SHIP_DATE).substring(0,10))
                    .segment(segment)
                    .build();
            reservationDemandTypeRequests.add(yantriksReservationDemandTypeRequest);

            List<YantriksLocationReservationDetailsRequest> locationReservationDetailsRequests = new ArrayList<>();
            YantriksLocationReservationDetailsRequest yantriksLocationReservationDetailsRequest = YantriksLocationReservationDetailsRequest.builder()
                    .locationId(currInvReservation.getAttribute(UrbanConstants.A_SHIP_NODE))
                    .locationType(yantriksUtil.getLocationType(currInvReservation.getAttribute(UrbanConstants.A_SHIP_NODE)))
                    .demands(reservationDemandTypeRequests)
                    .build();
            locationReservationDetailsRequests.add(yantriksLocationReservationDetailsRequest);

            YantriksLineReservationDetailsRequest yantriksLineReservationDetailsRequest = YantriksLineReservationDetailsRequest.builder()
                    .fulfillmentService(fulfillmentService)
                    .fulfillmentType(UrbanConstants.FT_SHIP)
                    .lineId(String.valueOf(counter))
                    .productId(currInvReservation.getAttribute(UrbanConstants.A_ITEM_ID))
                    .uom(currInvReservation.getAttribute(UrbanConstants.A_UOM))
                    .locationReservationDetails(locationReservationDetailsRequests)
                    .build();
            lineReservationDetailsRequests.add(yantriksLineReservationDetailsRequest);
            counter++;
        }
        YantriksReservationRequest yantriksReservationRequest = YantriksReservationRequest.builder()
                .expirationTime(expirationTime)
                .expirationTimeUnit(expirationTimeUnit)
                .orderId(firstInvReservation.getAttribute(UrbanConstants.A_RESERVATION_ID))
                .orgId(orgId)
                .updateTime(yantriksUtil.getCurrentDateOrTimeStamp(updateTimeFormatter))
                .updateUser(UrbanConstants.V_RT_URBN_USER)
                .lineReservationDetails(lineReservationDetailsRequests)
                .build();

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
