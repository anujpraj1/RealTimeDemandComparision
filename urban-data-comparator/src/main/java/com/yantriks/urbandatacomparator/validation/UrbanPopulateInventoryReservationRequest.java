package com.yantriks.urbandatacomparator.validation;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantriks.urbandatacomparator.model.YantriksLineReservationDetailsRequest;
import com.yantriks.urbandatacomparator.model.YantriksLocationReservationDetailsRequest;
import com.yantriks.urbandatacomparator.model.YantriksReservationDemandTypeRequest;
import com.yantriks.urbandatacomparator.model.YantriksReservationRequest;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@Slf4j
public class UrbanPopulateInventoryReservationRequest {


    @Value("${yantriks.default.fulfillmentservice}")
    private String fulfillmentService;

    @Value("${yantriks.default.orgid}")
    private String orgId;

    @Value("${yantriks.default.segment}")
    private String segment;

    @Autowired
    YantriksUtil yantriksUtil;


    public YantriksReservationRequest createReservationRequestFromInventoryReservation(Document inDoc) throws Exception {
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
        return YantriksReservationRequest.builder()
                .expirationTime(expirationTime)
                .expirationTimeUnit(expirationTimeUnit)
                .orderId(firstInvReservation.getAttribute(UrbanConstants.A_RESERVATION_ID))
                .orgId(orgId)
                .updateTime(yantriksUtil.getCurrentDateOrTimeStamp(updateTimeFormatter))
                .updateUser(UrbanConstants.V_RT_URBN_USER)
                .lineReservationDetails(lineReservationDetailsRequests)
                .build();
    }
}
