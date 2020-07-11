package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfs.core.YFSSystem;
import com.yantriks.urbandatacomparator.model.*;
import com.yantriks.urbandatacomparator.model.responses.YantriksAvailabilityErrorResponse;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import com.yantriks.urbandatacomparator.validation.UrbanPopulateInventoryReservationRequest;
import com.yantriks.urbandatacomparator.validation.UrbanPopulateOrderReservationRequest;
import com.yantriks.yih.adapter.util.YantriksCommonUtil;
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

    @Value("${apicall.newHttpClientCall}")
    private Boolean boolnewHttpClientCall;

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
        YantriksReservationResponse yantriksGetResponse = objMapper.readValue(reservationResponse, YantriksReservationResponse.class);
        log.debug("UrbanToYantriksCompareUpdate : YantriksReservationResponse : " + yantriksGetResponse);
        YantriksReservationRequest yantriksInRequest = null;
        String transactionType = UrbanConstants.TT_RESERVE;
        if (isUpdateFromInventoryReservation) {
            yantriksInRequest = urbanPopulateInventoryReservationRequest.createReservationRequestFromInventoryReservation(inDoc);
//            log.
            orderId = "";
            enterpriseCode = "";
        } else {
            yantriksInRequest = urbanPopulateOrderReservationRequest.createReservationRequestFromOrderListOP(inDoc);
            Element eleRoot = inDoc.getDocumentElement();
            Element eleOrder = SCXmlUtil.getChildElement(eleRoot, UrbanConstants.ELE_ORDER);
            orderId = eleOrder.getAttribute(UrbanConstants.A_ORDER_NO);
            enterpriseCode = eleOrder.getAttribute(UrbanConstants.A_ENTERPRISE_CODE);
            transactionType = determineTransactionType(yantriksInRequest.toString());
        }
        log.debug("UrbanToYantriksCompareUpdate: Reservation Request : " + yantriksInRequest);

        boolean areBothReservationsSame = true;
        if (!yantriksGetResponse.getOrderId().equals(yantriksInRequest.getOrderId())) {
            log.debug("Reservation Id did not match");
            areBothReservationsSame = false;
        }
        List<YantriksLineReservationDetailsRequest> yantriksInRequestLineDetails = yantriksInRequest.getLineReservationDetails();
        Map<String, YantriksLineReservationDetailsRequest> mapOfLineIdAndLineRequest = yantriksInRequestLineDetails.stream().collect(Collectors.toMap(YantriksLineReservationDetailsRequest::getLineId, currLineReservation -> currLineReservation));

        List<YantriksLineReservationDetailsResponse> yantriksGetResponseLineDetails = yantriksGetResponse.getLineReservationDetails();
        for (YantriksLineReservationDetailsResponse currGetLineResponse : yantriksGetResponseLineDetails) {
            if (mapOfLineIdAndLineRequest.containsKey(currGetLineResponse.getLineId())) {
                YantriksLineReservationDetailsRequest requestToMatch = mapOfLineIdAndLineRequest.get(currGetLineResponse.getLineId());
                if (matchLineLevelAttributes(currGetLineResponse, requestToMatch)) {
                    log.debug("Line Level Attributes are matching hence will check now on Location Reservation Details");
                    List<YantriksLocationReservationDetailsRequest> yantriksInLocations = requestToMatch.getLocationReservationDetails();
                    Map<String, YantriksLocationReservationDetailsRequest> mapInLocationAndLocationRequests = yantriksInLocations.stream().collect(Collectors.toMap(YantriksLocationReservationDetailsRequest::getLocationId,
                            yantriksLocationRequest -> yantriksLocationRequest));
                    List<YantriksLocationReservationDetailsResponse> yantriksLocationReservationDetailsResponses = currGetLineResponse.getLocationReservationDetails();
                    for (YantriksLocationReservationDetailsResponse currLocationResponse : yantriksLocationReservationDetailsResponses) {
                        if (mapInLocationAndLocationRequests.containsKey(currLocationResponse.getLocationId())) {
                            YantriksLocationReservationDetailsRequest locationRequestToMatch = mapInLocationAndLocationRequests.get(currLocationResponse.getLocationId());
                            if (matchLocationLevelAttributes(currLocationResponse, locationRequestToMatch)) {
                                log.debug("Location Level Attributes are matching for Current Location hence continuing with Demand comparison");
                                List<YantriksReservationDemandTypeRequest> yantriksReservationDemandTypeRequests = locationRequestToMatch.getDemands();
                                Map<String, YantriksReservationDemandTypeRequest> mapdemandTypeAndDemands = yantriksReservationDemandTypeRequests.stream()
                                        .collect(Collectors.toMap(YantriksReservationDemandTypeRequest::getUniqueKey, yantriksDemandRequest -> yantriksDemandRequest));
                                List<YantriksReservationDemandTypeResponse> yantriksReservationDemandTypeResponses = currLocationResponse.getDemands();
                                log.debug("content of map :: "+mapdemandTypeAndDemands.toString());
                                for (YantriksReservationDemandTypeResponse currDemandResponse : yantriksReservationDemandTypeResponses) {
                                    log.debug("currDemandResponse "+currDemandResponse.toString());
                                    log.debug("unique key : "+currDemandResponse.getUniqueKey());//.concat(":").concat(currDemandResponse.getReservationDate()));
                                    log.info(" reservation date "+currDemandResponse.getReservationDate());
                                   // if (mapdemandTypeAndDemands.containsKey(currDemandResponse.getDemandType().concat(":").concat(currDemandResponse.getReservationDate()))) {
                                    if (mapdemandTypeAndDemands.containsValue(currDemandResponse.getUniqueKey())) {
                                        YantriksReservationDemandTypeRequest demandRequestTomatch = mapdemandTypeAndDemands
                                                .get(currDemandResponse.getDemandType().concat(":").concat(currDemandResponse.getReservationDate()));
                                        if (matchDemandLevelAttributes(currDemandResponse, demandRequestTomatch)) {
                                            log.debug("Current Demand is matching");
                                        } else {
                                            log.debug("Current Demand did not match with the getOrderList Output Request either quantity/reservationdate/segment");
                                            areBothReservationsSame = false;
                                            break;
                                        }
                                    } else {
                                        log.debug("Demand type not found for specific reservation date in getOrderList Output hence breaking the loop");
                                        areBothReservationsSame = false;
                                        break;
                                    }
                                }
                            } else {
                                log.debug("Location Level Attributes did not match specifically location type");
                                areBothReservationsSame = false;
                                break;
                            }
                        } else {
                            log.debug("Reservation request does not have the Location ID : " + currLocationResponse.getLocationId());
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
                log.debug("Reservation Request does not have the line ID : " + currGetLineResponse.getLineId());
                areBothReservationsSame = false;
                break;
            }
        }


        if (compareAndGenerate) {
            log.debug("CompareAndGenerate Flag is turned on, Hence writing it into CSV file");
            urbanCsvOutputData.setExtnReservationId(yantriksInRequest.getOrderId());
            urbanCsvOutputData.setOrderId(orderId);
            urbanCsvOutputData.setEnterpriseCode(enterpriseCode);
            urbanCsvOutputData.setCompareAndGenerate(true);
            if (areBothReservationsSame) {
                urbanCsvOutputData.setReservationStatus(UrbanConstants.RS_MATCH);
            } else {
                urbanCsvOutputData.setReservationStatus(UrbanConstants.RS_MISMATCH);
            }
            log.debug("Logging for now instead of writing in CSV :: " + yantriksInRequest.toString());
        } else {
            log.debug("CompareAndUpdate Flag is turned on, Hence calling yantriks api to update in Yantriks");
            if (!areBothReservationsSame) {
                StringBuilder lineReserveUrl = new StringBuilder(UrbanConstants.YANTRIKS_LINE_RESERVE_URL);
                lineReserveUrl = urbanURI.getReservationUrl(lineReserveUrl, UrbanConstants.SC_GLOBAL, transactionType,
                        true, false, true, true);
                try {
                    ObjectMapper jsonObjMapper = new ObjectMapper();
                    String httpBody = jsonObjMapper.writeValueAsString(yantriksInRequest);
                    log.debug("HttpBody :: " + httpBody);
                    log.info("lineReserveUrl.toString()  "+lineReserveUrl.toString());

                    String inventoryAggURL = YantriksConstants.API_URL_INV_AVL;
                    boolean considerCapacityParam = false;
                    boolean considerGtinParam = false;
                    String apiUrl = inventoryAggURL + "?considerCapacity=" + considerCapacityParam + "&considerGtin=" + considerGtinParam + "&showAtpForHorizon" + "=" + true;
                    httpBody = httpBody.concat("\n   " + reservationResponse);

                    if (log.isDebugEnabled())
                        log.debug("httpBody is : " + httpBody);


                    String content = httpBody.substring(1, httpBody.length() - 1);
                    String newHttpClientCall = "";//YFSSystem.getProperty(YantriksConstants.PROP_ENABLE_CLOSEABLE_HTTP_CLIENT);
                    String response = null;
                    if (!YFCObject.isVoid(newHttpClientCall)) {
                        boolnewHttpClientCall = Boolean.valueOf(newHttpClientCall);
                    }
                    log.debug("Boolean Value : "+boolnewHttpClientCall);
                    if (boolnewHttpClientCall) {
                        log.debug("Boolean value to call new HTTP Client is true hence calling via closeable client");
                        response = yantriksUtil.callYantriksAPIV3(apiUrl, YantriksConstants.YIH_REQ_METHOD_POST,  httpBody,  YantriksCommonUtil.getAvailabilityProduct());
                    }
                    else{
                         response = yantriksUtil.callYantriksAPI(lineReserveUrl.toString(), UrbanConstants.HTTP_METHOD_PUT, httpBody, UrbanConstants.V_PRODUCT_YAS);
                    }
                    if (YantriksConstants.V_FAILURE.equals(response)) {
                        log.debug("UrbanToYantriksOrderDirectUpdate: Yantriks Reservation Call failed with FAILURE response hence will write the request in file");
                        log.debug("UrbanToYantriksOrderDirectUpdate: Writing the request in file");
                        ObjectMapper objOut = new ObjectMapper();
                        YantriksAvailabilityErrorResponse yantriksAvailabilityErrorResponse = objOut.readValue(reservationResponse, YantriksAvailabilityErrorResponse.class);
                        urbanCsvOutputData.setExtnReservationId(yantriksInRequest.getOrderId());
                        urbanCsvOutputData.setOrderId(orderId);
                        urbanCsvOutputData.setEnterpriseCode(enterpriseCode);
                        urbanCsvOutputData.setCompareAndGenerate(false);
                        urbanCsvOutputData.setReservationResponseCode(yantriksAvailabilityErrorResponse.getStatus());
                        urbanCsvOutputData.setError(yantriksAvailabilityErrorResponse.getError());
                        urbanCsvOutputData.setMessage(yantriksAvailabilityErrorResponse.getMessage());
                    } else {
                        urbanCsvOutputData.setExtnReservationId(yantriksInRequest.getOrderId());
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
                    urbanCsvOutputData.setExtnReservationId(yantriksInRequest.getOrderId());
                    urbanCsvOutputData.setOrderId(orderId);
                    urbanCsvOutputData.setEnterpriseCode(enterpriseCode);
                    urbanCsvOutputData.setCompareAndGenerate(false);
                    urbanCsvOutputData.setReservationResponseCode(UrbanConstants.RC_500);
                    urbanCsvOutputData.setError(UrbanConstants.ERR_YANT_SERVER_DOWN);
                    urbanCsvOutputData.setMessage("");
                }
            }
        }
        if (null == urbanCsvOutputData.getExtnReservationId()) {
            log.info("ExtnReservationID is not set, that means Comparison result was Match");
            urbanCsvOutputData.setExtnReservationId(yantriksInRequest.getOrderId());
            urbanCsvOutputData.setOrderId(orderId);
            urbanCsvOutputData.setEnterpriseCode(enterpriseCode);
            urbanCsvOutputData.setCompareAndGenerate(true);
            urbanCsvOutputData.setReservationStatus(UrbanConstants.MSG_NO_UPDATE_REQUIRED);
        }
        return urbanCsvOutputData;
    }

    private String determineTransactionType(String yantriksInRequest) {
        log.debug("yantriksInRequest "+(yantriksInRequest.toString()));
        if(yantriksInRequest.toString().contains("SCHEDULE_TO")){
            log.debug(UrbanConstants.TT_TRANSFER);
            return UrbanConstants.TT_TRANSFER;
        }
        else{
            return UrbanConstants.TT_RESERVE;
        }
    }

    private boolean matchDemandLevelAttributes(YantriksReservationDemandTypeResponse currDemandResponse, YantriksReservationDemandTypeRequest demandRequestTomatch) {
        log.debug("currDemandResponse "+currDemandResponse.toString());
        log.debug("demandRequestTomatch "+demandRequestTomatch.toString());
        return currDemandResponse.getQuantity() == demandRequestTomatch.getQuantity() &&
                currDemandResponse.getReservationDate().equals(demandRequestTomatch.getReservationDate()) &&
                currDemandResponse.getSegment().equals(demandRequestTomatch.getSegment());
    }

    private boolean matchLocationLevelAttributes(YantriksLocationReservationDetailsResponse currLocationResponse, YantriksLocationReservationDetailsRequest locationRequestToMatch) {
        return (currLocationResponse.getLocationType() == locationRequestToMatch.getLocationType() || currLocationResponse.getLocationType().equals(locationRequestToMatch.getLocationType()));
    }

    private boolean matchLineLevelAttributes(YantriksLineReservationDetailsResponse currLineResponse, YantriksLineReservationDetailsRequest requestToMatch) {
        return currLineResponse.getProductId().equals(requestToMatch.getProductId()) &&
                currLineResponse.getUom().equals(requestToMatch.getUom()) &&
                currLineResponse.getFulfillmentType().equals(requestToMatch.getFulfillmentType()) ;
                //&& currLineResponse.getFulfillmentService().equals(requestToMatch.getFulfillmentService());
    }
}
