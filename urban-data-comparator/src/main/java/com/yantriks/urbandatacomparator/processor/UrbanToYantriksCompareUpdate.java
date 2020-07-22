package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantriks.urbandatacomparator.configuration.ReservationClient;
import com.yantriks.urbandatacomparator.model.*;
import com.yantriks.urbandatacomparator.model.request.ReservationDemandTypeRequest;
import com.yantriks.urbandatacomparator.model.request.ReservationOrderLineRequest;
import com.yantriks.urbandatacomparator.model.request.ReservationOrderRequest;
import com.yantriks.urbandatacomparator.model.request.ReservationProductLocationRequest;
import com.yantriks.urbandatacomparator.model.responses.*;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import com.yantriks.urbandatacomparator.validation.UrbanPopulateInventoryReservationRequestGenerator;
import com.yantriks.urbandatacomparator.validation.UrbanPopulateOrderReservationRequest;
import com.yantriks.yih.adapter.util.YantriksConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UrbanToYantriksCompareUpdate {

    @Autowired
    ObjectMapper objectMapper;

    @Value("${data.mode.comparegenerate}")
    private Boolean compareAndGenerate;

    @Value("${data.mode.compareupdate}")
    private Boolean compareAndUpdate;

    @Value("${apicall.newHttpClientCall}")
    private Boolean boolnewHttpClientCall;

    @Value("${yantriks.default.orgid}")
    private String orgId;

    @Autowired
    ReservationClient reservationClient;

    @Autowired
    YantriksUtil yantriksUtil;

    @Autowired
    UrbanURI urbanURI;

    @Autowired
    UrbanPopulateOrderReservationRequest urbanPopulateOrderReservationRequest;

    @Autowired
    UrbanPopulateInventoryReservationRequestGenerator urbanPopulateInventoryReservationRequest;

    @Autowired
    UrbanDataCompareProcessor urbanDataCompareProcessor;

//    @Autowired
//    UrbanCsvOutputData urbanCsvOutputData;

    public UrbanCsvOutputData compareReservationsAndUpdate(Document inDoc, ReservationOrderResponse yantriksGetResponse, boolean isUpdateFromInventoryReservation, String reservationId) throws Exception {

        String orderId = null;
        String enterpriseCode = null;
//        ReservationOrderResponse yantriksGetResponse = objectMapper.readValue(reservationResponse, ReservationOrderResponse.class);
        log.debug("UrbanToYantriksCompareUpdate : YantriksReservationResponse : " + yantriksGetResponse);
        ReservationOrderRequest yantriksInRequest = null;
        String transactionType = UrbanConstants.TT_RESERVE;
        if (isUpdateFromInventoryReservation) {
            yantriksInRequest = urbanPopulateInventoryReservationRequest.createReservationRequestFromInventoryReservation(inDoc);
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
        List<ReservationOrderLineRequest> yantriksInRequestLineDetails = yantriksInRequest.getLineReservationDetails();
        Map<String, ReservationOrderLineRequest> mapOfLineIdAndLineRequest = yantriksInRequestLineDetails.stream().collect(Collectors.toMap(ReservationOrderLineRequest::getLineId, currLineReservation -> currLineReservation));

        List<ReservationOrderLineResponse> yantriksGetResponseLineDetails = yantriksGetResponse.getLineReservationDetails();
        for (ReservationOrderLineResponse currGetLineResponse : yantriksGetResponseLineDetails) {
            if (mapOfLineIdAndLineRequest.containsKey(currGetLineResponse.getLineId())) {
                ReservationOrderLineRequest requestToMatch = mapOfLineIdAndLineRequest.get(currGetLineResponse.getLineId());
                if (matchLineLevelAttributes(currGetLineResponse, requestToMatch)) {
                    log.debug("Line Level Attributes are matching hence will check now on Location Reservation Details");
                    List<ReservationProductLocationRequest> yantriksInLocations = requestToMatch.getLocationReservationDetails();
                    Map<String, ReservationProductLocationRequest> mapInLocationAndLocationRequests = yantriksInLocations.stream().collect(Collectors.toMap(ReservationProductLocationRequest::getLocationId,
                            yantriksLocationRequest -> yantriksLocationRequest));
                    List<ReservationProductLocationResponse> reservationProductLocationRespons = currGetLineResponse.getLocationReservationDetails();
                    for (ReservationProductLocationResponse currLocationResponse : reservationProductLocationRespons) {
                        if (mapInLocationAndLocationRequests.containsKey(currLocationResponse.getLocationId())) {
                            ReservationProductLocationRequest locationRequestToMatch = mapInLocationAndLocationRequests.get(currLocationResponse.getLocationId());
                            if (matchLocationLevelAttributes(currLocationResponse, locationRequestToMatch)) {
                                log.debug("Location Level Attributes are matching for Current Location hence continuing with Demand comparison");
                                List<ReservationDemandTypeRequest> reservationDemandTypeRequests = locationRequestToMatch.getDemands();
                                Map<String, ReservationDemandTypeRequest> mapdemandTypeAndDemands = reservationDemandTypeRequests.stream()
                                        .collect(Collectors.toMap(ReservationDemandTypeRequest::getUniqueKey, yantriksDemandRequest -> yantriksDemandRequest));
                                List<ReservationDemandTypeResponse> reservationDemandTypeRespons = currLocationResponse.getDemands();
                                log.debug("content of map :: " + mapdemandTypeAndDemands.toString());
                                for (ReservationDemandTypeResponse currDemandResponse : reservationDemandTypeRespons) {
                                    log.debug("currDemandResponse " + currDemandResponse.toString());
                                    log.debug("unique key : " + currDemandResponse.getUniqueKey());//.concat(":").concat(currDemandResponse.getReservationDate()));
                                    log.info(" reservation date " + currDemandResponse.getReservationDate());
                                    // if (mapdemandTypeAndDemands.containsKey(currDemandResponse.getDemandType().concat(":").concat(currDemandResponse.getReservationDate()))) {
                                    if (mapdemandTypeAndDemands.containsValue(currDemandResponse.getUniqueKey())) {
                                        ReservationDemandTypeRequest demandRequestTomatch = mapdemandTypeAndDemands
                                                .get(currDemandResponse.getUniqueKey());
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


        UrbanCsvOutputData urbanCsvOutputData = new UrbanCsvOutputData();


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
                StringBuilder lineReserveUrl = new StringBuilder(UrbanConstants.YANTRIKS_RESERVE_URL);
                lineReserveUrl = urbanURI.getReservationUrl(lineReserveUrl, UrbanConstants.SC_GLOBAL, transactionType,
                        true, false, true, true);
                try {
                    String httpBody = objectMapper.writeValueAsString(yantriksInRequest);
                    log.debug("HttpBody :: " + httpBody);
                    log.info("lineReserveUrl.toString()  " + lineReserveUrl.toString());

                    String inventoryAggURL = YantriksConstants.API_URL_INV_AVL;
                    boolean considerCapacityParam = false;
                    boolean considerGtinParam = false;


                    if (log.isDebugEnabled())
                        log.debug("httpBody is : " + httpBody);

                    log.debug("cancelling reservation");
//                    StringBuilder cancelReservationUrl = new StringBuilder(YantriksConstants.API_URL_GET_RESERVE_DETAILS);
//                    cancelReservationUrl = urbanURI.getReservationURLForCancelReservation(cancelReservationUrl, orgId, reservationId, false);
//                    log.debug("cancelReservationUrl " + cancelReservationUrl.toString());
//                    String response = yantriksUtil.callYantriksGetOrDeleteAPI(cancelReservationUrl.toString(), "DELETE", UrbanConstants.V_PRODUCT_YAS);
                    ResponseEntity<Void> response2 = reservationClient.deleteReservation(reservationId, false);
//                    String response = response2.toString();
                    log.debug("reservation cancelled , response received " + response2.getBody());
                    urbanCsvOutputData.setExtnReservationId(yantriksInRequest.getOrderId());
                    urbanCsvOutputData.setOrderId(orderId);
                    urbanCsvOutputData.setEnterpriseCode(enterpriseCode);

                    if(response2.getStatusCodeValue()==200) {
                        ReservationOrderRequest reservationOrderRequest = this.objectMapper.readValue(httpBody, ReservationOrderRequest.class);
                        log.debug(" yantriksReservationRequest final " + reservationOrderRequest.toString());
                        if (!reservationOrderRequest.getLineReservationDetails().isEmpty()) {
                            log.debug("mkaing a POST call to update the reservation lineReserveUrl " + lineReserveUrl.toString() + " UrbanConstants.HTTP_METHOD_POST " + UrbanConstants.HTTP_METHOD_POST + " httpBody :" + httpBody + " UrbanConstants.V_PRODUCT_YAS) :" + UrbanConstants.V_PRODUCT_YAS);
                            //HttpResponseImpl resvResponse = yantriksUtil.callYantriksAPI(lineReserveUrl.toString(), UrbanConstants.HTTP_METHOD_POST, httpBody, UrbanConstants.V_PRODUCT_YAS);
                            ResponseEntity<ReservationProductLocationResponse> response = reservationClient.createReservation(UrbanConstants.SC_GLOBAL, transactionType, reservationOrderRequest, true, false, false, true);
                            urbanDataCompareProcessor.processYantriksReservationResponse(response,inDoc,yantriksGetResponse,reservationId,false);
                            urbanCsvOutputData.setCompareAndGenerate(false);
                            urbanCsvOutputData.setReservationResponseCode(response.getStatusCodeValue());
                            urbanCsvOutputData.setError(null);
                            urbanCsvOutputData.setMessage(response.getStatusCode().getReasonPhrase());
//                            yantriksUtil.populateCSVData(csvWriteData, urbanCsvOutputData);
                        }
                        else{
                            log.debug("status 0");
                            urbanCsvOutputData.setCompareAndGenerate(false);
                            urbanCsvOutputData.setError("setting status 0");
                        }
                    }
                    else{
                        throw new RuntimeException("Delete reservation failed");
                    }

                } catch (Exception e) {
                    YantriksUtil.updateOutputDataWithException(urbanCsvOutputData, e);

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
        log.debug("yantriksInRequest " + (yantriksInRequest.toString()));
        if (yantriksInRequest.toString().contains("SCHEDULE_TO")) {
            log.debug(UrbanConstants.TT_TRANSFER);
            return UrbanConstants.TT_TRANSFER;
        } else {
            return UrbanConstants.TT_RESERVE;
        }
    }

    private boolean matchDemandLevelAttributes(ReservationDemandTypeResponse currDemandResponse, ReservationDemandTypeRequest demandRequestTomatch) {
        log.debug("currDemandResponse " + currDemandResponse.toString());
        log.debug("demandRequestTomatch " + demandRequestTomatch.toString());
        return currDemandResponse.getQuantity() == demandRequestTomatch.getQuantity() &&
                currDemandResponse.getReservationDate().equals(demandRequestTomatch.getReservationDate()) &&
                currDemandResponse.getSegment().equals(demandRequestTomatch.getSegment());
    }

    private boolean matchLocationLevelAttributes(ReservationProductLocationResponse currLocationResponse, ReservationProductLocationRequest locationRequestToMatch) {
        return (currLocationResponse.getLocationType() == locationRequestToMatch.getLocationType() || currLocationResponse.getLocationType().equals(locationRequestToMatch.getLocationType()));
    }

    private boolean matchLineLevelAttributes(ReservationOrderLineResponse currLineResponse, ReservationOrderLineRequest requestToMatch) {
        return currLineResponse.getProductId().equals(requestToMatch.getProductId()) &&
                currLineResponse.getUom().equals(requestToMatch.getUom()) &&
                currLineResponse.getFulfillmentType().equals(requestToMatch.getFulfillmentType());
        //&& currLineResponse.getFulfillmentService().equals(requestToMatch.getFulfillmentService());
    }
}
