package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantriks.urbandatacomparator.model.*;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import com.yantriks.urbandatacomparator.validation.UrbanPopulateInventoryReservationRequest;
import com.yantriks.urbandatacomparator.validation.UrbanPopulateOrderReservationRequest;
import com.yantriks.yih.adapter.util.YantriksConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UrbanToYantriksCompareUpdate {

    @Value("${data.mode.comparegenerate}")
    private Boolean compareAndGenerate;

    @Value("${data.mode.compareupdate}")
    private Boolean compareAndUpdate;

    @Autowired
    YantriksUtil yantriksUtil;

    @Autowired
    UrbanURI urbanURI;

    @Autowired
    UrbanPopulateOrderReservationRequest urbanPopulateOrderReservationRequest;

    @Autowired
    UrbanPopulateInventoryReservationRequest urbanPopulateInventoryReservationRequest;

    @Autowired
    UrbanCsvOutputData urbanCsvOutputData;

    public UrbanCsvOutputData compareReservationsAndUpdate(Document inDoc, String reservationResponse, boolean isUpdateFromInventoryReservation) throws Exception {

        String orderId = null;
        String enterpriseCode = null;
        ObjectMapper objMapper = new ObjectMapper();
        YantriksReservationResponse yantriksReservationResponse = objMapper.readValue(reservationResponse, YantriksReservationResponse.class);
        log.debug("UrbanToYantriksCompareUpdate : YantriksReservationResponse : " + yantriksReservationResponse);
        YantriksReservationRequest yantriksReservationRequest = null;
        String transactionType = UrbanConstants.TT_RESERVE;
        if (isUpdateFromInventoryReservation) {
            yantriksReservationRequest = urbanPopulateInventoryReservationRequest.createReservationRequestFromInventoryReservation(inDoc);
            orderId = "";
            enterpriseCode = "";
        } else {
            yantriksReservationRequest = urbanPopulateOrderReservationRequest.createReservationRequestFromOrderListOP(inDoc);
            Element eleRoot = inDoc.getDocumentElement();
            Element eleOrder = SCXmlUtil.getChildElement(eleRoot, UrbanConstants.ELE_ORDER);
            orderId = eleOrder.getAttribute(UrbanConstants.A_ORDER_NO);
            enterpriseCode = eleOrder.getAttribute(UrbanConstants.A_ENTERPRISE_CODE);
            transactionType = determineTransactionType(eleOrder);
        }
        log.debug("UrbanToYantriksCompareUpdate: Reservation Request : " + yantriksReservationRequest);

        boolean areBothReservationsSame = true;
        if (!yantriksReservationResponse.getOrderId().equals(yantriksReservationRequest.getOrderId())) {
            log.debug("Reservation Id did not match");
            areBothReservationsSame = false;
        }
        List<YantriksLineReservationDetailsRequest> yantriksLineReservationRequestList = yantriksReservationRequest.getLineReservationDetails();
        Map<String, YantriksLineReservationDetailsRequest> mapOfLineIdAndLineRequest = yantriksLineReservationRequestList.stream().collect(Collectors.toMap(YantriksLineReservationDetailsRequest::getLineId, currLineReservation -> currLineReservation));

        List<YantriksLineReservationDetailsResponse> yantriksLineReservationDetailsResponses = yantriksReservationResponse.getLineReservationDetails();
        for (YantriksLineReservationDetailsResponse currLineResponse : yantriksLineReservationDetailsResponses) {
            if (mapOfLineIdAndLineRequest.containsKey(currLineResponse.getLineId())) {
                YantriksLineReservationDetailsRequest requestToMatch = mapOfLineIdAndLineRequest.get(currLineResponse.getLineId());
                if (matchLineLevelAttributes(currLineResponse, requestToMatch)) {
                    log.debug("Line Level Attributes are matching hence will check now on Location Reservation Details");
                    List<YantriksLocationReservationDetailsRequest> yantriksLocationReservationDetailsRequests = requestToMatch.getLocationReservationDetails();
                    Map<String, YantriksLocationReservationDetailsRequest> mapLocationAndLocationRequests = yantriksLocationReservationDetailsRequests.stream().collect(Collectors.toMap(YantriksLocationReservationDetailsRequest::getLocationId,
                            yantriksLocationRequest -> yantriksLocationRequest));
                    List<YantriksLocationReservationDetailsResponse> yantriksLocationReservationDetailsResponses = currLineResponse.getLocationReservationDetails();
                    for (YantriksLocationReservationDetailsResponse currLocationResponse : yantriksLocationReservationDetailsResponses) {
                        if (mapLocationAndLocationRequests.containsKey(currLocationResponse.getLocationId())) {
                            YantriksLocationReservationDetailsRequest locationRequestToMatch = mapLocationAndLocationRequests.get(currLocationResponse.getLocationId());
                            if (matchLocationLevelAttributes(currLocationResponse, locationRequestToMatch)) {
                                log.debug("Location Level Attributes are matching for Current Location hence continuing with Demand comparision");
                                List<YantriksReservationDemandTypeRequest> yantriksReservationDemandTypeRequests = locationRequestToMatch.getDemands();
                                Map<String, YantriksReservationDemandTypeRequest> mapdemandTypeAndDemands = yantriksReservationDemandTypeRequests.stream()
                                        .collect(Collectors.toMap(YantriksReservationDemandTypeRequest::getDemandType, yantriksDemandRequest -> yantriksDemandRequest));
                                List<YantriksReservationDemandTypeResponse> yantriksReservationDemandTypeResponses = currLocationResponse.getDemands();
                                for (YantriksReservationDemandTypeResponse currDemandResponse : yantriksReservationDemandTypeResponses) {
                                    if (mapdemandTypeAndDemands.containsKey(currDemandResponse.getDemandType())) {
                                        YantriksReservationDemandTypeRequest demandRequestTomatch = mapdemandTypeAndDemands.get(currDemandResponse.getDemandType());
                                        if (matchDemandLevelAttributes(currDemandResponse, demandRequestTomatch)) {
                                            log.debug("Current Demand is matching");
                                        } else {
                                            log.debug("Current Demand did not match with the getOrderList Output Request");
                                            areBothReservationsSame = false;
                                            break;
                                        }
                                    } else {
                                        log.debug("Demand type not found in getOrderList Output hence breaking the loop");
                                        break;
                                    }
                                }
                            } else {
                                log.debug("Location Level Attributes did not match");
                                areBothReservationsSame = false;
                                break;
                            }
                        } else {
                            log.debug("Yantriks Reservation does not have the Location ID : " + currLocationResponse.getLocationId());
                            areBothReservationsSame = false;
                            break;
                        }
                    }
                } else {
                    log.debug("Line Level Details are not matching either productId/uom/fulfillmentType/fulfillmentService");
                    areBothReservationsSame = false;
                    break;
                }
            } else {
                log.debug("Yantriks Reservation does not have the line ID : " + currLineResponse.getLineId());
                areBothReservationsSame = false;
                break;
            }
        }


        if (compareAndGenerate) {
            log.debug("CompareAndGenerate Flag is turned on, Hence writing it into CSV file");
            urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
            urbanCsvOutputData.setOrderId(orderId);
            urbanCsvOutputData.setEnterpriseCode(enterpriseCode);
            urbanCsvOutputData.setCompareAndGenerate(true);
            if (areBothReservationsSame) {
                urbanCsvOutputData.setReservationStatus(UrbanConstants.RS_MATCH);
            } else {
                urbanCsvOutputData.setReservationStatus(UrbanConstants.RS_MISMATCH);
            }
            log.debug("Logging for now instead of writing in CSV :: " + yantriksReservationRequest.toString());

        } else {
            log.debug("CompareAndUpdate Flag is turned on, Hence calling yantriks api to update in Yantriks");
            if (!areBothReservationsSame) {
                StringBuilder lineReserveUrl = new StringBuilder(UrbanConstants.YANTRIKS_LINE_RESERVE_URL);
                lineReserveUrl = urbanURI.getReservationUrl(lineReserveUrl, UrbanConstants.SC_GLOBAL, transactionType,
                        true, false, true, false);
                try {
                    ObjectMapper jsonObjMapper = new ObjectMapper();
                    String httpBody = jsonObjMapper.writeValueAsString(yantriksReservationRequest);
                    log.debug("HttpBody :: " + httpBody);
                    String response = yantriksUtil.callYantriksAPI(lineReserveUrl.toString(), UrbanConstants.HTTP_METHOD_POST, httpBody, UrbanConstants.V_PRODUCT_YAS);
                    if (YantriksConstants.V_FAILURE.equals(response)) {
                        log.debug("UrbanToYantriksOrderDirectUpdate: Yantriks Reservation Call failed with FAILURE response hence will write the request in file");
                        log.debug("UrbanToYantriksOrderDirectUpdate: Writing the request in file");
                        urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
                        urbanCsvOutputData.setOrderId(orderId);
                        urbanCsvOutputData.setEnterpriseCode(enterpriseCode);
                        urbanCsvOutputData.setCompareAndGenerate(false);
                        urbanCsvOutputData.setReservationResponseCode(0);
                        urbanCsvOutputData.setError("");
                        urbanCsvOutputData.setMessage("");
                    } else {
                        urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
                        urbanCsvOutputData.setOrderId(orderId);
                        urbanCsvOutputData.setEnterpriseCode(enterpriseCode);
                        urbanCsvOutputData.setCompareAndGenerate(false);
                        urbanCsvOutputData.setReservationResponseCode(UrbanConstants.RC_200);
                        urbanCsvOutputData.setError("");
                        urbanCsvOutputData.setMessage(UrbanConstants.MSG_SUCCESS);
                    }
                } catch (Exception e) {
                    log.error("UrbanToYantriksOrderDirectUpdate : Exception caught while creating reservation : " + e.getMessage());
                    log.debug("UrbanToYantriksOrderDirectUpdate: Writing the request in file");
                    urbanCsvOutputData.setExtnReservationId(yantriksReservationRequest.getOrderId());
                    urbanCsvOutputData.setOrderId(orderId);
                    urbanCsvOutputData.setEnterpriseCode(enterpriseCode);
                    urbanCsvOutputData.setCompareAndGenerate(false);
                    urbanCsvOutputData.setReservationResponseCode(UrbanConstants.RC_500);
                    urbanCsvOutputData.setError(UrbanConstants.ERR_YANT_SERVER_DOWN);
                    urbanCsvOutputData.setMessage("");
                }
            }
        }
        return urbanCsvOutputData;
    }

    private String determineTransactionType(Element eleOrder) {
        String maxOrderStatus = eleOrder.getAttribute(UrbanConstants.A_MAX_ORDER_STATUS);
        if (UrbanConstants.IM_LIST_ALLOCATED_STATUSES.contains(maxOrderStatus)) {
            return UrbanConstants.TT_RELEASE;
        } else if (UrbanConstants.IM_LIST_SCHEDULED_STATUSES.contains(maxOrderStatus)) {
            return UrbanConstants.TT_SCHEDULE;
        } else if (UrbanConstants.IM_LIST_OPEN_STATUSES.contains(maxOrderStatus)) {
            return UrbanConstants.TT_RESERVE;
        } else if (UrbanConstants.IM_LIST_BACKORDER_STATUSES.contains(maxOrderStatus)) {
            return UrbanConstants.TT_SCHEDULE;
        } else {
            return UrbanConstants.TT_RESERVE;
        }
    }

    private boolean matchDemandLevelAttributes(YantriksReservationDemandTypeResponse currDemandResponse, YantriksReservationDemandTypeRequest demandRequestTomatch) {
        return currDemandResponse.getQuantity() == demandRequestTomatch.getQuantity() &&
                currDemandResponse.getReservationDate().equals(demandRequestTomatch.getReservationDate()) &&
                currDemandResponse.getSegment().equals(demandRequestTomatch.getSegment());
    }

    private boolean matchLocationLevelAttributes(YantriksLocationReservationDetailsResponse currLocationResponse, YantriksLocationReservationDetailsRequest locationRequestToMatch) {
        return currLocationResponse.getLocationType().equals(locationRequestToMatch.getLocationType());
    }

    private boolean matchLineLevelAttributes(YantriksLineReservationDetailsResponse currLineResponse, YantriksLineReservationDetailsRequest requestToMatch) {
        return currLineResponse.getProductId().equals(requestToMatch.getProductId()) &&
                currLineResponse.getUom().equals(requestToMatch.getUom()) &&
                currLineResponse.getFulfillmentType().equals(requestToMatch.getFulfillmentType()) &&
                currLineResponse.getFulfillmentService().equals(requestToMatch.getFulfillmentService());
    }
}
