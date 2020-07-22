package com.yantriks.urbandatacomparator.validation;


import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantriks.urbandatacomparator.model.request.ReservationOrderLineRequest;
import com.yantriks.urbandatacomparator.model.request.ReservationProductLocationRequest;
import com.yantriks.urbandatacomparator.model.request.ReservationDemandTypeRequest;
import com.yantriks.urbandatacomparator.model.request.ReservationOrderRequest;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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


    public ReservationOrderRequest createReservationRequestFromInventoryReservation(Document inDoc) throws Exception {
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
        List<ReservationOrderLineRequest> lineReservationDetailsRequests = new ArrayList<>();
        for (int i = 0; i< nlInvReservations.getLength(); i++) {
            Element currInvReservation = (Element) nlInvReservations.item(i);
            Element eleItem = SCXmlUtil.getChildElement(currInvReservation, UrbanConstants.E_ITEM);
            String itemId = eleItem.getAttribute(UrbanConstants.A_ITEM_ID);
            String shipNode = currInvReservation.getAttribute(UrbanConstants.A_SHIP_NODE);
            String locationType = yantriksUtil.getLocationType(currInvReservation.getAttribute(UrbanConstants.A_SHIP_NODE));
            String shipDate = currInvReservation.getAttribute(UrbanConstants.A_SHIP_DATE);
            String qty = currInvReservation.getAttribute(UrbanConstants.A_QUANTITY);
            int qtyToPut = (int) Double.parseDouble(qty);

            boolean existInLineReservation = checkAndUpdateLineReservations(lineReservationDetailsRequests, itemId, shipNode, shipDate.substring(0,10), locationType, qtyToPut);
            if (!existInLineReservation) {
                List<ReservationDemandTypeRequest> reservationDemandTypeRequests = new ArrayList<>();
                ReservationDemandTypeRequest reservationDemandTypeRequest = ReservationDemandTypeRequest.builder()
                        .demandType(UrbanConstants.DT_RESERVED)
                        .quantity(qtyToPut)
                        .reservationDate(currInvReservation.getAttribute(UrbanConstants.A_SHIP_DATE).substring(0,10))
                        .segment(segment)
                        .build();
                reservationDemandTypeRequests.add(reservationDemandTypeRequest);

                List<ReservationProductLocationRequest> locationReservationDetailsRequests = new ArrayList<>();
                ReservationProductLocationRequest reservationProductLocationRequest = ReservationProductLocationRequest.builder()
                        .locationId(shipNode)
                        .locationType(locationType)
                        .demands(reservationDemandTypeRequests)
                        .build();
                log.debug("yantriksLineReservationDetailsRequest.getLocationReservationDetails()"+ reservationProductLocationRequest.getDemands().isEmpty());
                locationReservationDetailsRequests.add(reservationProductLocationRequest);

                ReservationOrderLineRequest reservationOrderLineRequest = ReservationOrderLineRequest.builder()
                        .fulfillmentService(fulfillmentService)
                        .fulfillmentType(UrbanConstants.FT_SHIP)
                        .lineId(String.valueOf(counter))
                        .productId(eleItem.getAttribute(UrbanConstants.A_ITEM_ID))
                        .uom(eleItem.getAttribute(UrbanConstants.A_UOM))
                        .locationReservationDetails(locationReservationDetailsRequests)
                        .build();
                log.debug("yantriksLineReservationDetailsRequest.getLocationReservationDetails()"+ reservationOrderLineRequest.getLocationReservationDetails().isEmpty());
                log.debug("Populating LINE Request");
                lineReservationDetailsRequests.add(reservationOrderLineRequest);
                counter++;
            }
        }
        return ReservationOrderRequest.builder()
                .expirationTime(new Long(expirationTime))
                .expirationTimeUnit(TimeUnit.valueOf(expirationTimeUnit))
                .orderId(firstInvReservation.getAttribute(UrbanConstants.A_RESERVATION_ID))
                .orgId(orgId)
              //  .updateTime(yantriksUtil.getCurrentDateOrTimeStamp(updateTimeFormatter))
                .updateTime(ZonedDateTime.now())
                .updateUser(UrbanConstants.V_RT_URBN_USER)
                .lineReservationDetails(lineReservationDetailsRequests)
                .build();
    }

    private boolean checkAndUpdateLineReservations(List<ReservationOrderLineRequest> lineReservationDetailsRequests, String itemId, String shipNode, String shipDate, String locationType, int qtyToPut) {
        AtomicBoolean linePresent = new AtomicBoolean(false);
        lineReservationDetailsRequests.stream()
                .forEach(lineReservationDetailsRequest -> {
                    if (lineReservationDetailsRequest.getProductId().equals(itemId)) {
                        if (!checkUpdateOrAddLocationReservations(lineReservationDetailsRequest.getLocationReservationDetails(), shipNode, shipDate, qtyToPut)) {
                            log.debug("Did not find Location Hence adding one");
                            List<ReservationDemandTypeRequest> demands = new ArrayList<>();
                            ReservationDemandTypeRequest reservationDemandTypeRequest = ReservationDemandTypeRequest.builder()
                                    .demandType(UrbanConstants.DT_RESERVED)
                                    .quantity(qtyToPut)
                                    .reservationDate(shipDate)
                                    .segment(segment)
                                    .build();
                            demands.add(reservationDemandTypeRequest);

                            ReservationProductLocationRequest reservationProductLocationRequest =                                reservationProductLocationRequest = ReservationProductLocationRequest.builder()
                                        .locationId(shipNode)
                                        .locationType(locationType)
                                        .demands(demands)
                                        .build();
                            lineReservationDetailsRequest.getLocationReservationDetails().add(reservationProductLocationRequest);
                        }
                        linePresent.set(true);
                    }
                });
        return linePresent.get();
    }

    private boolean checkUpdateOrAddLocationReservations(List<ReservationProductLocationRequest> locationReservationDetails, String shipNode, String shipDate, int qtyToPut) {
        AtomicBoolean locationPresent = new AtomicBoolean(false);
        locationReservationDetails.forEach(locationReservationDetail -> {
            if (locationReservationDetail.getLocationId().equals(shipNode)) {
                addDemandDetails(locationReservationDetail.getDemands(), shipDate, qtyToPut);
                locationPresent.set(true);
            }
        });
        return locationPresent.get();
    }

    private void addDemandDetails(List<ReservationDemandTypeRequest> demands, String shipDate, int qtyToPut) {
        log.debug("Adding the Demand Details");
                    ReservationDemandTypeRequest reservationDemandTypeRequest = ReservationDemandTypeRequest.builder()
                            .demandType(UrbanConstants.DT_RESERVED)
                            .quantity(qtyToPut)
                            .reservationDate(shipDate)
                            .segment(segment)
                            .build();
                    demands.add(reservationDemandTypeRequest);
    }
}
