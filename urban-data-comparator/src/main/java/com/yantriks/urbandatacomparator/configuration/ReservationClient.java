package com.yantriks.urbandatacomparator.configuration;


import com.yantriks.urbandatacomparator.model.request.ReservationOrderRequest;
import com.yantriks.urbandatacomparator.model.responses.ReservationProductLocationResponse;

import com.yantriks.urbandatacomparator.model.responses.ReservationOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.yantriks.urbandatacomparator.util.UrbanConstants.*;
import static com.yantriks.yih.adapter.util.YantriksConstants.QUERY_PARAM_RESTORE_CAPACITY;

@FeignClient(
        url = "${urban.yantriks.protocol}"+"://"+"${urban.yantriks.availability.host2}",
        name = "reservation",
        path = ReservationClient.ENDPOINT,
        decode404 = true,
        configuration = CommonFeignConfig.class
)
public interface ReservationClient {

    public static final String ENDPOINT = "/availability-services/reservations/v3.0";

    @RequestMapping(method = RequestMethod.GET, value = "/URBN/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReservationOrderResponse> getReservationForOrder(
            @PathVariable("orderId") String orderId
    );

    @RequestMapping(method = RequestMethod.GET, value = "/URBN/{reservationId}",
            consumes = "application/json",
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ReservationOrderResponse> getReservation(
            @PathVariable("reservationId") String reservationId

    );

    @RequestMapping(method = RequestMethod.POST, value = "/{sellingChannel}/{transactionType}")
    public ResponseEntity<ReservationProductLocationResponse> createReservation(
            @PathVariable("sellingChannel") String sellingChannel,
            @PathVariable("transactionType") String transactionType,
            @RequestBody ReservationOrderRequest request,
            @RequestParam(CAN_RESERVE_AFTER) boolean canReserveAfter,
            @RequestParam(CONSIDER_CAPACITY) boolean considerCapacity,
            @RequestParam(CONSIDER_GTIN) boolean considerGtin,
            @RequestParam(IGNORE_AVAILABILITY_CHECK) boolean ignoreAvailabilityCheck

    );

    @RequestMapping(method = RequestMethod.PUT, value = "/{sellingChannel}/{transactionType}")
    public ResponseEntity<ReservationProductLocationResponse> updateReservation(
            @PathVariable("sellingChannel") String sellingChannel,
            @PathVariable("transactionType") String transactionType,
            @RequestBody ReservationOrderRequest request,
            @RequestParam(CAN_RESERVE_AFTER) boolean canReserveAfter,
            @RequestParam(CONSIDER_CAPACITY) boolean considerCapacity,
            @RequestParam(CONSIDER_GTIN) boolean considerGtin,
            @RequestParam(IGNORE_AVAILABILITY_CHECK) boolean ignoreAvailabilityCheck

    );


    @RequestMapping(method = RequestMethod.PUT, value = "/lines/{sellingChannel}/{transactionType}")
    public ResponseEntity<ReservationOrderResponse> updateReservationOrderLines(
            @PathVariable("sellingChannel") String sellingChannel,
            @PathVariable("transactionType") String transactionType,
            @RequestBody ReservationOrderRequest request,
            @RequestParam(CAN_RESERVE_AFTER) boolean canReserveAfter,
            @RequestParam(CONSIDER_CAPACITY) boolean considerCapacity,
            @RequestParam(CONSIDER_GTIN) boolean considerGtin,
            @RequestParam(IGNORE_AVAILABILITY_CHECK) boolean ignoreAvailabilityCheck,
            @RequestParam("integrationService") boolean integrationService

    );


    @RequestMapping(method = RequestMethod.DELETE, value = "/URBN/{orderNo}",
            consumes = "application/json",
            produces = "application/json")
    public ResponseEntity<Void> deleteReservation(
            @PathVariable("orderNo") String orderNo,
            @RequestParam(QUERY_PARAM_RESTORE_CAPACITY) boolean restoreCapacity
    );

    @RequestMapping(method = RequestMethod.DELETE, value = "/lines/URBN/{orderId}/{lineId}")
    public ResponseEntity<Void> cancelReservationOrderLine(
            @PathVariable("orderId") String orderId,
            @PathVariable("lineId") String lineId,
            @RequestParam(required = false, defaultValue = "false") boolean restoreCapacity);
}

