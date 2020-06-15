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

import java.text.SimpleDateFormat;
import java.util.*;

@Component
@Slf4j
public class UrbanPopulateOrderReservationRequest {


    @Value("${yantriks.default.fulfillmentservice}")
    private String fulfillmentService;

    @Value("${yantriks.default.orgid}")
    private String orgId;

    @Value("${yantriks.default.segment}")
    private String segment;

    @Autowired
    YantriksUtil yantriksUtil;

    public String getDemandTypeForCurrentStatus(String status) {
        log.debug("Yantriks Util : Checking appropriate demand type for status");
        if (UrbanConstants.IM_LIST_OPEN_STATUSES.contains(status)) {
            return UrbanConstants.DT_OPEN;
        } else if (UrbanConstants.IM_LIST_SCHEDULED_STATUSES.contains(status)) {
            return UrbanConstants.DT_SCHEDULED;
        } else if (UrbanConstants.IM_LIST_ALLOCATED_STATUSES.contains(status)) {
            return UrbanConstants.DT_ALLOCATED;
        } else if (UrbanConstants.IM_LIST_BACKORDER_STATUSES.contains(status)) {
            return UrbanConstants.DT_BACKORDERED;
        } else {
            return null;
        }
    }

    public YantriksReservationRequest createReservationRequestFromOrderListOP(Document inDoc) {
        Element eleRoot = inDoc.getDocumentElement();
        Element eleOrder = SCXmlUtil.getChildElement(eleRoot, UrbanConstants.ELE_ORDER);
        Element eleOrderLines = SCXmlUtil.getChildElement(eleOrder, UrbanConstants.E_ORDER_LINES);
        NodeList nlOrderLines = eleOrderLines.getElementsByTagName(UrbanConstants.E_ORDER_LINE);
        int orderLinesLen = nlOrderLines.getLength();
        List<YantriksLineReservationDetailsRequest> lineReservationDetailsRequests = new ArrayList<>();
        for (int i = 0; i < orderLinesLen; i++) {
            Element currOrderLine = (Element) nlOrderLines.item(i);
            Element eleItem = SCXmlUtil.getChildElement(currOrderLine, UrbanConstants.E_ITEM);
            Element eleOrderStatuses = SCXmlUtil.getChildElement(currOrderLine, UrbanConstants.E_ORDER_STATUSES);
            NodeList nlOrderStatuses = eleOrderStatuses.getElementsByTagName(UrbanConstants.E_ORDER_STATUS);
            int nlOrderStatusesLength = nlOrderStatuses.getLength();
            Map<String, String> scheduleKeyToStatusMap = new HashMap<>();
            for (int j = 0; j < nlOrderStatusesLength; j++) {
                Element currOrderStatus = (Element) nlOrderStatuses.item(j);
                String orderLineSchKey = currOrderStatus.getAttribute(UrbanConstants.A_ORDER_LINE_SCHEDULE_KEY);
                String status = currOrderStatus.getAttribute(UrbanConstants.A_STATUS);
                log.debug("Putting in the map OrderLineScheduleKey : " + orderLineSchKey + " Status : " + status);
                scheduleKeyToStatusMap.put(orderLineSchKey, status);
            }
            Element eleSchedules = SCXmlUtil.getChildElement(currOrderLine, UrbanConstants.A_SCHEDULES);
            NodeList nlSchedules = eleSchedules.getElementsByTagName(UrbanConstants.A_SCHEDULE);
            int scheduleLength = nlSchedules.getLength();
            Map<String, List<Element>> shipNodeToSchedule = new HashMap<>();
            for (int k = 0; k < scheduleLength; k++) {
                log.debug("Fetching the Current Schedule and checking weather it exists in created map scheduleKeyToStatusMap");
                Element currSchedule = (Element) nlSchedules.item(k);
                String orderLineScheduleKey = currSchedule.getAttribute(UrbanConstants.A_ORDER_LINE_SCHEDULE_KEY);
                String shipNode = currSchedule.getAttribute(UrbanConstants.A_SHIP_NODE);
                if (scheduleKeyToStatusMap.containsKey(orderLineScheduleKey)) {
                    String statusFromMap = scheduleKeyToStatusMap.get(orderLineScheduleKey);
                    log.debug("Map has the schedule key and schedule too hence will populate map key as shipnode and value as map of status and quantity");
                    if (shipNodeToSchedule.containsKey(shipNode)) {
                        log.debug("ShipNodeToStatusQtyMap already has the shipnode so will update the existing one");
                        List<Element> existingScheduleList = shipNodeToSchedule.get(shipNode);
                        currSchedule.setAttribute(UrbanConstants.A_STATUS, statusFromMap);
                        existingScheduleList.add(currSchedule);
                    } else {
                        log.debug("ShipNodeToStatusQtyMap does not have shipnode so will insert new status and quantity entry");
                        List<Element> scheduleList = new ArrayList<>();
                        currSchedule.setAttribute(UrbanConstants.A_STATUS, statusFromMap);
                        scheduleList.add(currSchedule);
                        log.debug("SHIP Node :: "+shipNode);
                        log.debug("Schedule :: "+SCXmlUtil.getString(currSchedule));
                        shipNodeToSchedule.put(shipNode, scheduleList);
                    }
                }
            }

            log.debug("Map which is going to be utilised for creating deamnds w.r.t to shipnodes : " + shipNodeToSchedule);
            List<YantriksLocationReservationDetailsRequest> locationReservationDetailsRequests = new ArrayList<>();
            shipNodeToSchedule.entrySet()
                    .stream()
                    .forEach(e -> {
                        List<Element> scheduleList = e.getValue();
                        List<YantriksReservationDemandTypeRequest> reservationDemandTypeRequests = new ArrayList<>();
                        scheduleList.stream()
                                .forEach(element -> {
                                    String quantity = element.getAttribute(UrbanConstants.A_QUANTITY);
                                    int intQty = (int) Double.parseDouble(quantity);
                                    String statusOfDemand = element.getAttribute(YantriksConstants.A_STATUS);
                                    String demandType = getDemandTypeForCurrentStatus(statusOfDemand);
                                    log.debug("UrbanToyantriksOrderDirectUpdate : Demand Type Returned : " + demandType);
                                    if (!UrbanConstants.IM_LIST_SHIPPED_STATUSES.contains(statusOfDemand) || null != demandType) {
                                        YantriksReservationDemandTypeRequest yantriksReservationDemandTypeRequest = YantriksReservationDemandTypeRequest.builder()
                                                .demandType(demandType)
                                                .quantity(intQty)
                                                .reservationDate(element.getAttribute(UrbanConstants.A_EXP_SHIP_DATE).substring(0, 10))
                                                .segment(segment)
                                                .build();
                                        log.debug("Current Demand Adding :: "+yantriksReservationDemandTypeRequest.toString());
                                        log.debug("Current Demand Adding :: "+yantriksReservationDemandTypeRequest);
                                        reservationDemandTypeRequests.add(yantriksReservationDemandTypeRequest);
                                    } else {
                                        log.debug("Status found : " + statusOfDemand + " Hence did not create a demand for it");
                                    }
                                });
                        YantriksLocationReservationDetailsRequest yantriksLocationReservationDetailsRequest = null;
                        try {
                            yantriksLocationReservationDetailsRequest = YantriksLocationReservationDetailsRequest.builder()
                                    .locationId(e.getKey().equals("")?"NETWORK":e.getKey())
                                    .locationType(e.getKey().equals("")?"NETWORK":yantriksUtil.getLocationType(e.getKey()))
                                    .demands(reservationDemandTypeRequests)
                                    .build();
                        } catch (Exception ex) {
                            log.error("Exception Caught while determining the location type : " + ex.getMessage());
                        }
                        log.debug("EMPTY CHECK FOR DEMAND "+reservationDemandTypeRequests.isEmpty());
                        if (!reservationDemandTypeRequests.isEmpty()) {
                            locationReservationDetailsRequests.add(yantriksLocationReservationDetailsRequest);
                        }
                    });
            YantriksLineReservationDetailsRequest yantriksLineReservationDetailsRequest = YantriksLineReservationDetailsRequest.builder()
                    .fulfillmentService(fulfillmentService)
                    .fulfillmentType(UrbanConstants.FT_SHIP)
                    .lineId(currOrderLine.getAttribute(UrbanConstants.A_PRIME_LINE_NO))
                    .productId(eleItem.getAttribute(UrbanConstants.A_ITEM_ID))
                    .uom(eleItem.getAttribute(UrbanConstants.A_UOM))
                    .locationReservationDetails(locationReservationDetailsRequests)
                    .build();
            log.debug("EMPTY CHECK FOR LOCATION "+locationReservationDetailsRequests.isEmpty());
            if (!locationReservationDetailsRequests.isEmpty()) {
                lineReservationDetailsRequests.add(yantriksLineReservationDetailsRequest);
            }
        }

        SimpleDateFormat updateTimeFormatter = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

        return YantriksReservationRequest.builder()
                .expirationTime(0)
                .expirationTimeUnit(UrbanConstants.V_SECONDS)
                .orderId(yantriksUtil.getReservationID(eleOrder))
                .orgId(orgId)
                .updateTime(yantriksUtil.getCurrentDateOrTimeStamp(updateTimeFormatter))
                .updateUser(UrbanConstants.V_RT_URBN_USER)
                .lineReservationDetails(lineReservationDetailsRequests)
                .build();

    }

}
