package com.yantriks.urbandatacomparator.validation;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
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
public class UrbanPopulateOrderReservationRequest {


    @Value("${yantriks.default.fulfillmentservice}")
    private String fulfillmentService;

    @Value("${yantriks.default.orgid}")
    private String orgId;

    @Value("${yantriks.default.segment}")
    private String segment;

    @Autowired
    YantriksUtil yantriksUtil;

    public String getDemandTypeForCurrentStatus(String status,String strShipNode,String strLineType,Boolean isProcureFromNodePresent) {

        String strStatusTemp = status;
        if(status.contains(".")){
            int indexOfDot = status.indexOf(".");
            status = status.substring(0,indexOfDot);
            log.debug("strVal after formatting "+status);
        }

             Integer iStatus = (int) Double.parseDouble(status);

        log.debug("iStatus :"+iStatus);
        log.debug("isProcureFromNodePresent :"+isProcureFromNodePresent);
        log.debug("strLineType :"+strLineType);
            if(strLineType.equalsIgnoreCase("FURNITURE") && Boolean.TRUE.equals(isProcureFromNodePresent)){

                if(UrbanConstants.IM_LIST_TO_TERMINAL_STATUSES.contains(strStatusTemp)){
                    log.debug(" procurement TO received  :"+strStatusTemp);
                    return "SCHEDULED";
                }
//                if(iStatus>=1500 && iStatus <2500)
//                {
//                    return "SCHEDULED";
//                }
                if(UrbanConstants.IM_LIST_TO_INITIAL_STATUSES.contains(strStatusTemp)){
                    log.debug(" TO in progress :"+strStatusTemp);
                    return "SCHEDULED_TO";
                }
//                if(iStatus>=2500 && iStatus<3200)
//                { log.info("iStatus "+iStatus);
//                    return "SCHEDULED_TO";
//                }
            }
            if(iStatus >=1500 && iStatus <3200){
                return "SCHEDULED";
            }
            if((!strShipNode.equalsIgnoreCase("NETWORK")) && iStatus<1100){
                return "RESERVED";
            }
            if(strShipNode.equalsIgnoreCase("NETWORK") || iStatus==1100){
                return "OPEN";
            }
            if(iStatus==1300){
                return "BACKORDER";
            }
            else if(iStatus>=3200) {
                return "ALLOCATED";
            }
        return null;
    }

    public YantriksReservationRequest createReservationRequestFromOrderListOP(Document inDoc) {

        Element eleRoot = inDoc.getDocumentElement();
        Element eleOrder = SCXmlUtil.getChildElement(eleRoot, UrbanConstants.ELE_ORDER);
        Element eleOrderLines = SCXmlUtil.getChildElement(eleOrder, UrbanConstants.E_ORDER_LINES);
        NodeList nlOrderLines = eleOrderLines.getElementsByTagName(UrbanConstants.E_ORDER_LINE);
        int orderLinesLen = nlOrderLines.getLength();
        List<YantriksLineReservationDetailsRequest> lineReservationDetailsRequests = new ArrayList<>();

        String strFulfillmentType = null;

        for (int i = 0; i < orderLinesLen; i++) {
            Element currOrderLine = (Element) nlOrderLines.item(i);
            Element eleItem = SCXmlUtil.getChildElement(currOrderLine, UrbanConstants.E_ITEM);
            Element eleOrderStatuses = SCXmlUtil.getChildElement(currOrderLine, UrbanConstants.E_ORDER_STATUSES);
            NodeList nlOrderStatuses = eleOrderStatuses.getElementsByTagName(UrbanConstants.E_ORDER_STATUS);

            String strLineType = currOrderLine.getAttribute("LineType");
            String strProcureFromNode = null;
            strFulfillmentType = currOrderLine.getAttribute("FulfillmentType");

            int nlOrderStatusesLength = nlOrderStatuses.getLength();
            Map<String, String> scheduleKeyToStatusMap = new HashMap<>();
            for (int j = 0; j < nlOrderStatusesLength; j++) {
                Element currOrderStatus = (Element) nlOrderStatuses.item(j);
                String orderLineSchKey = currOrderStatus.getAttribute(UrbanConstants.A_ORDER_LINE_SCHEDULE_KEY);
                Element eleDetails = SCXmlUtil.getChildElement(currOrderStatus,"Details");
                if(!YFCObject.isVoid(eleDetails)){
                    strProcureFromNode =eleDetails.getAttribute("ProcureFromNode");
                    log.debug(" strProcureFromNode "+strProcureFromNode);
                }
                String status = currOrderStatus.getAttribute(UrbanConstants.A_STATUS);
                log.debug("Putting in the map OrderLineScheduleKey : " + orderLineSchKey + " Status : " + status);
                scheduleKeyToStatusMap.put(orderLineSchKey, status);
            }
            Element eleSchedules = SCXmlUtil.getChildElement(currOrderLine, UrbanConstants.A_SCHEDULES);
            NodeList nlSchedules = eleSchedules.getElementsByTagName(UrbanConstants.A_SCHEDULE);
            int scheduleLength = nlSchedules.getLength();
            Map<String, List<Element>> shipNodeToSchedule = new HashMap<>();
            Boolean isProcureFromNodePresent = false;
            for (int k = 0; k < scheduleLength; k++) {
                log.debug("Fetching the Current Schedule and checking weather it exists in created map scheduleKeyToStatusMap");
                Element currSchedule = (Element) nlSchedules.item(k);
                String orderLineScheduleKey = currSchedule.getAttribute(UrbanConstants.A_ORDER_LINE_SCHEDULE_KEY);
                log.debug("currSchedule "+SCXmlUtil.getString(currSchedule));
                log.debug("scheduleKeyToStatusMap "+scheduleKeyToStatusMap);
                log.debug("orderLineScheduleKey :"+orderLineScheduleKey);
                if (scheduleKeyToStatusMap.containsKey(orderLineScheduleKey)) {
                    String statusFromMap = scheduleKeyToStatusMap.get(orderLineScheduleKey);
//                    String strStatusTemp = statusFromMap;
                    log.debug("statusFromMap : "+statusFromMap);
                    if(!YFCObject.isVoid(strProcureFromNode)){
                        isProcureFromNodePresent = true;
                    }
//                    if(UrbanConstants.IM_LIST_SHIPPED_STATUSES.contains(statusFromMap)){
//                        statusFromMap="3700";
//                    }
//                    if(statusFromMap.contains(".")){
//                        int indexOfDot = statusFromMap.indexOf(".");
//                        statusFromMap = statusFromMap.substring(0,indexOfDot);
//                        log.debug("strVal after formatting "+statusFromMap);
//                    }

//                    Integer iStatus = (int) Double.parseDouble(statusFromMap);
                    String strShipNode = null;
                    if(strLineType.equalsIgnoreCase("FURNITURE") && Boolean.TRUE.equals(isProcureFromNodePresent) && statusFromMap.compareTo("2500")<0){
                        strShipNode = strProcureFromNode;
                        log.debug("equating procurement node to shipnode" +strShipNode +"------"+strProcureFromNode);
                    }
                    else{
                        strShipNode = currSchedule.getAttribute(UrbanConstants.A_SHIP_NODE);
                    }
                    log.debug("Map has the schedule key and schedule too hence will populate map key as shipnode and value as map of status and quantity");
                    if (shipNodeToSchedule.containsKey(strShipNode)) {
                        log.debug("ShipNodeToStatusQtyMap already has the shipnode so will update the existing one");
                        List<Element> existingScheduleList = shipNodeToSchedule.get(strShipNode);
                        currSchedule.setAttribute(UrbanConstants.A_STATUS, statusFromMap);
                        currSchedule.setAttribute("LineType",strLineType);
                        currSchedule.setAttribute("ProcureFromNode",strProcureFromNode);
                        existingScheduleList.add(currSchedule);
                    } else {
                        log.debug("ShipNodeToStatusQtyMap does not have shipnode so will insert new status and quantity entry");
                        List<Element> scheduleList = new ArrayList<>();
                        currSchedule.setAttribute(UrbanConstants.A_STATUS, statusFromMap);
                        currSchedule.setAttribute("LineType",strLineType);
                        scheduleList.add(currSchedule);
                        log.debug("SHIP Node :: "+strShipNode);
                        log.debug("Schedule :: "+SCXmlUtil.getString(currSchedule));
                        shipNodeToSchedule.put(strShipNode, scheduleList);
                    }
                }
            }

            log.debug("Map which is going to be utilised for creating deamnds w.r.t to shipnodes : " + shipNodeToSchedule);
            List<YantriksLocationReservationDetailsRequest> locationReservationDetailsRequests = new ArrayList<>();
            Boolean finalIsProcureFromNodePresent = isProcureFromNodePresent;
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
                                    String strShipNode=e.getKey();
                                    String demandType = getDemandTypeForCurrentStatus(statusOfDemand,strShipNode,strLineType, finalIsProcureFromNodePresent);
                                    log.debug("UrbanToyantriksOrderDirectUpdate : Demand Type Returned : " + demandType);
                                    String strFormattedESDate = null;
                                    log.debug("statusOfDemand: "+statusOfDemand);
                                    boolean isESDPastDate=false;
                                    if (!UrbanConstants.IM_LIST_SHIPPED_STATUSES.contains(statusOfDemand) && null != demandType) {
                                        log.debug("entered inside");
                                        String strExpectedShipmentDate = element.getAttribute(UrbanConstants.A_EXP_SHIP_DATE);
                                        if(YFCObject.isVoid(strExpectedShipmentDate)){
                                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                                           strExpectedShipmentDate =  yantriksUtil.getCurrentDateOrTimeStamp(format);
                                        }
                                        else{
                                            try {
                                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                                                String strCurrentDate = yantriksUtil.getCurrentDateOrTimeStamp(format);
                                                log.debug("strCurrentDate :"+strCurrentDate);
                                                log.debug("strExpectedShipmentDate :"+strExpectedShipmentDate);
                                               strFormattedESDate =   yantriksUtil.getDateinUTC(strExpectedShipmentDate);
                                               Date ESDate = format.parse(strExpectedShipmentDate);
                                               Date currentDate = format.parse(strCurrentDate);
                                               if(ESDate.compareTo(currentDate)<0){ //if ship date is before system date or past date
                                                   strFormattedESDate=strCurrentDate;
                                                   log.debug("date is less than sysdate or date is pastDate, hence not appending anything");
                                               }
                                            } catch (ParseException ex) {
                                                ex.printStackTrace();
                                            }
                                        }

                                        boolean isDemandTypeAlreadyAdded = false;
                                       for(int count=0 ;count<reservationDemandTypeRequests.size();count++){
                                           YantriksReservationDemandTypeRequest yantriksReservationDemandTypeRequest = reservationDemandTypeRequests.get(count);
                                           if(demandType.equals(yantriksReservationDemandTypeRequest.getDemandType())){
                                               int currQty = yantriksReservationDemandTypeRequest.getQuantity();
                                               yantriksReservationDemandTypeRequest.setQuantity(currQty+intQty);
                                               log.debug("demand type already present , hence only increasing quantity");
                                               isDemandTypeAlreadyAdded = true;
                                           }
                                       }

                                       if(Boolean.FALSE.equals(isDemandTypeAlreadyAdded)){
                                           YantriksReservationDemandTypeRequest yantriksReservationDemandTypeRequest = YantriksReservationDemandTypeRequest.builder()
                                                   .demandType(demandType)
                                                   .quantity(intQty)
                                                   .reservationDate(strFormattedESDate)
                                                   .segment(segment)
                                                   .build();
                                           log.debug("Current Demand Adding :: "+yantriksReservationDemandTypeRequest);
                                           reservationDemandTypeRequests.add(yantriksReservationDemandTypeRequest);
                                       }

                                    } else {
                                        log.debug("Status found : " + statusOfDemand + " Hence did not create a demand for it");
                                    }
                                });
                        YantriksLocationReservationDetailsRequest yantriksLocationReservationDetailsRequest = null;
                        try {
                            yantriksLocationReservationDetailsRequest = YantriksLocationReservationDetailsRequest.builder()
                                    .locationId(e.getKey().equals("")?"NETWORK":e.getKey())
                                    .locationType(e.getKey().equals("")?"NETWORK":yantriksUtil.
                                            getLocationType(e.getKey()))
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

             if(strFulfillmentType.equalsIgnoreCase("STS")){
                 strFulfillmentType = UrbanConstants.FT_STS;
             }
             else if(strFulfillmentType.equalsIgnoreCase("ISPU")){
                 strFulfillmentType = UrbanConstants.FT_ISPU;
             }
             else{
                 strFulfillmentType = UrbanConstants.FT_SHIP;
             }
            YantriksLineReservationDetailsRequest yantriksLineReservationDetailsRequest = YantriksLineReservationDetailsRequest.builder()
                   // .fulfillmentService(fulfillmentService)
                    .fulfillmentType(strFulfillmentType)
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
                .expirationTime(180)
                .expirationTimeUnit(UrbanConstants.V_MINUTES)
                .orderId(yantriksUtil.getReservationID(eleOrder))
                .orgId(orgId)
                .updateTime(yantriksUtil.getCurrentDateOrTimeStamp(updateTimeFormatter))
                .updateUser(UrbanConstants.V_RT_URBN_USER)
                .lineReservationDetails(lineReservationDetailsRequests)
                .build();

    }

}
