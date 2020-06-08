package com.yantriks.urbandatacomparator.processor;

import com.yantriks.urbandatacomparator.model.UrbanCsvData;
import com.yantriks.urbandatacomparator.model.UrbanCsvOutputData;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetInvListCall;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetOrderListCall;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.urbandatacomparator.util.YantriksUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Slf4j
@Component
public class UrbanDataCompareProcessor implements Processor {

    @Autowired
    SterlingGetInvListCall sterlingGetInvListCall;

    @Autowired
    SterlingGetOrderListCall sterlingGetOrderListCall;


    @Autowired
    UrbanToYantriksInvDirectUpdate urbanToYantriksInvDirectUpdate;

    @Autowired
    UrbanToYantriksOrderDirectUpdate urbanToYantriksOrderDirectUpdate;

    @Autowired
    UrbanToYantriksCompareUpdate urbanToYantriksCompareUpdate;

    @Autowired
    YantriksUtil yantriksUtil;


    @Override
    public void process(Exchange exchange) throws Exception {
        UrbanCsvData csvData = exchange.getIn().getBody(UrbanCsvData.class);
        UrbanCsvOutputData urbanCsvOutputData = new UrbanCsvOutputData();
        StringBuilder csvWriteData = new StringBuilder();
        log.debug("UrbanDataCompareProcessor: CSV Data Input");
        log.debug("OrderId : " + csvData.getOrderId());
        log.debug("EnterpriseCode : " + csvData.getEnterpriseCode());
        log.debug("ReservationId : " + csvData.getReservationId());
        String orderId = csvData.getOrderId();
        String enterpriseCode = csvData.getEnterpriseCode();
        String reservationId = csvData.getReservationId();
        boolean populatedOnce = false;

        StringBuilder reservationUrl = new StringBuilder(UrbanConstants.YANTRIKS_GET_RESERVE_URL);
        reservationUrl.append("/");
        reservationUrl.append(UrbanConstants.V_ORGID_URBN);
        reservationUrl.append("/");
        reservationUrl.append(reservationId);

        log.debug("UrbanDataCompareProcessor: Reservation Id present hence first check would be getInventoryReservationList");
        Document getInventoryReservationList = sterlingGetInvListCall.executeGetInvListApi(reservationId);
        if (getInventoryReservationList.getDocumentElement().hasChildNodes()) {
            log.debug("UrbanDataCompareProcessor: Reservation exist in Sterling which means order is not created hence needs to be checked against yantriks");
            String reservationResponse = null;
            try {
                //reservationResponse = yantriksUtil.callYantriksGetOrDeleteAPI(reservationUrl.toString(), UrbanConstants.HTTP_METHOD_GET, UrbanConstants.V_PRODUCT_YAS);
                reservationResponse = "{\n" +
                        "    \"updateTime\": \"2020-06-08T18:25:16.790Z\",\n" +
                        "    \"updateUser\": \"RTURBNUSER\",\n" +
                        "    \"orgId\": \"URBN\",\n" +
                        "    \"expirationTime\": \"2500-01-01T06:12:22.107\",\n" +
                        "    \"expirationTimeUnit\": \"HOURS\",\n" +
                        "    \"orderId\": \"Something_TEST\",\n" +
                        "    \"orderType\": null,\n" +
                        "    \"lineReservationDetails\": [\n" +
                        "        {\n" +
                        "            \"lineId\": \"1\",\n" +
                        "            \"fulfillmentService\": \"STANDARD\",\n" +
                        "            \"fulfillmentType\": \"Ship\",\n" +
                        "            \"orderLineRef\": null,\n" +
                        "            \"productId\": \"TEST_ITEM\",\n" +
                        "            \"uom\": \"TEST\",\n" +
                        "            \"locationReservationDetails\": [\n" +
                        "                {\n" +
                        "                    \"locationId\": \"TEST_NODE\",\n" +
                        "                    \"locationType\": null,\n" +
                        "                    \"demands\": [\n" +
                        "                        {\n" +
                        "                            \"demandType\": \"RESERVED\",\n" +
                        "                            \"reservationDate\": \"2020-06-11\",\n" +
                        "                            \"segment\": \"DEFAULT\",\n" +
                        "                            \"quantity\": 2.0\n" +
                        "                        }\n" +
                        "                    ]\n" +
                        "                },\n" +
                        "                {\n" +
                        "                    \"locationId\": \"TEST_NODE_NO_INVENTORY\",\n" +
                        "                    \"locationType\": null,\n" +
                        "                    \"demands\": [\n" +
                        "                        {\n" +
                        "                            \"demandType\": \"RESERVED\",\n" +
                        "                            \"reservationDate\": \"2020-06-12\",\n" +
                        "                            \"segment\": \"DEFAULT\",\n" +
                        "                            \"quantity\": 2.0\n" +
                        "                        }\n" +
                        "                    ]\n" +
                        "                }\n" +
                        "            ]\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}";
            } catch (Exception e) {
                e.printStackTrace();
                reservationResponse = UrbanConstants.V_EXC_FAILURE;
            }
            String response = yantriksUtil.determineErrorOrSuccessOnReservationPost(reservationResponse);
            if (UrbanConstants.V_FAILURE.equals(response)) {
                log.debug("UrbanDataCompareProcessor: Reservation does not exist hence creating a new one from inventory response");
                urbanCsvOutputData = urbanToYantriksInvDirectUpdate.directUpdateToYantriks(getInventoryReservationList);
                log.debug("UrbanDataCompareProcessor: directUpdateToInvYantriks : Done");
            } else if (UrbanConstants.V_EXC_FAILURE.equals(response)) {
                log.debug("UrbanDataCompareProcessor :: Yantriks API failed with Exception hence will set the data to write in CSV");
                yantriksUtil.defaultDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, "GET_RESERVATION_FAILED");
                populatedOnce = true;
            } else {
                log.debug("UrbanDataCompareProcessor: Comparing both reservation and getInventoryReservationList output, generating report or/and updating the yantriks");
                urbanCsvOutputData = urbanToYantriksCompareUpdate.compareReservationsAndUpdate(getInventoryReservationList, reservationResponse, true);
            }
        } else {
            log.debug("UrbanDataCompareProcessor: No Reservation found hence will check and call getOrderList ");
            if (isEmptyOrNull(orderId) || isEmptyOrNull(enterpriseCode)) {
                log.error("UrbanDataCompareProcessor: Reservation Id was blank and either order is NA or enterprisecode is NA hence subsequent comparision can't be made");
                log.error("There can be a possibility that soft reservation is expired or Order is created but orderid and enterprisecode is not passed");
            } else {
                log.debug("UrbanDataCompareProcessor: Calling getOrderList API of sterling");
                Document getOrderListOP = sterlingGetOrderListCall.executeGetOLListApi(orderId, enterpriseCode);
                String reservationResponse = null;
                try {
                    //reservationResponse = yantriksUtil.callYantriksGetOrDeleteAPI(reservationUrl.toString(), UrbanConstants.HTTP_METHOD_GET, UrbanConstants.V_PRODUCT_YAS);
                    reservationResponse = "{\n" +
                            "    \"updateTime\": \"2020-06-08T10:01:47.539Z\",\n" +
                            "    \"updateUser\": \"RTURBNUSER\",\n" +
                            "    \"orgId\": \"URBN\",\n" +
                            "    \"expirationTime\": null,\n" +
                            "    \"expirationTimeUnit\": \"SECONDS\",\n" +
                            "    \"orderId\": \"Y100005600\",\n" +
                            "    \"orderType\": null,\n" +
                            "    \"lineReservationDetails\": [\n" +
                            "        {\n" +
                            "            \"lineId\": \"1\",\n" +
                            "            \"fulfillmentService\": \"STANDARD\",\n" +
                            "            \"fulfillmentType\": \"Ship\",\n" +
                            "            \"orderLineRef\": null,\n" +
                            "            \"productId\": \"item_based_item\",\n" +
                            "            \"uom\": \"EACH\",\n" +
                            "            \"locationReservationDetails\": [\n" +
                            "                {\n" +
                            "                    \"locationId\": \"NETWORK\",\n" +
                            "                    \"locationType\": \"NETWORK\",\n" +
                            "                    \"demands\": [\n" +
                            "                        {\n" +
                            "                            \"demandType\": \"OPEN\",\n" +
                            "                            \"reservationDate\": \"2020-06-08\",\n" +
                            "                            \"segment\": \"DEFAULT\",\n" +
                            "                            \"quantity\": 1.0\n" +
                            "                        }\n" +
                            "                    ]\n" +
                            "                }\n" +
                            "            ]\n" +
                            "        }\n" +
                            "    ]\n" +
                            "}";
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Setting it UP");
                    reservationResponse = UrbanConstants.V_EXC_FAILURE;
                }
                String response = yantriksUtil.determineErrorOrSuccessOnReservationPost(reservationResponse);
                System.out.println("Response :: " + response);
                if (UrbanConstants.V_FAILURE.equals(response)) {
                    log.debug("UrbanDataCompareProcessor: Yantriks does not have reservation hence based on getOrderList call output updating yantriks");
                    urbanCsvOutputData = urbanToYantriksOrderDirectUpdate.directUpdateToYantriks(getOrderListOP);
                } else if (UrbanConstants.V_EXC_FAILURE.equals(response)) {
                    log.debug("UrbanDataCompareProcessor :: Yantriks API failed with Exception hence will set the data to write in CSV");
                    yantriksUtil.defaultDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, "GET_RESERVATION_FAILED");
                    populatedOnce = true;
                } else {
                    log.debug("UrbanDataCompareProcessor: Comparing both reservation and getOrderList Output, generating report or/and updating the yantriks");
                    urbanCsvOutputData = urbanToYantriksCompareUpdate.compareReservationsAndUpdate(getOrderListOP, reservationResponse, false);
                }
            }
        }
        log.info("UrbanDataCompareProcessor : Setting the output to be written to CSV");
        System.out.println("CSV Output Data :: " + urbanCsvOutputData.toString());
        if (urbanCsvOutputData.getExtnReservationId() != null || urbanCsvOutputData.getOrderId() != null) {
            csvWriteData.append(urbanCsvOutputData.getOrderId());
            csvWriteData.append("|");
            csvWriteData.append(urbanCsvOutputData.getEnterpriseCode());
            csvWriteData.append("|");
            csvWriteData.append(urbanCsvOutputData.getExtnReservationId());
            csvWriteData.append("|");
            if (urbanCsvOutputData.isCompareAndGenerate()) {
                csvWriteData.append(urbanCsvOutputData.getReservationStatus());
            } else {
                csvWriteData.append(urbanCsvOutputData.getReservationResponseCode());
                csvWriteData.append("|");
                csvWriteData.append(urbanCsvOutputData.getError());
                csvWriteData.append("|");
                csvWriteData.append(urbanCsvOutputData.getMessage());
            }
        } else {
            if (!populatedOnce) {
                yantriksUtil.defaultDataToPopulate(csvWriteData, reservationId, enterpriseCode, orderId, "DATA_INCORRECT");
            }
        }
        exchange.getIn().setBody(csvWriteData.toString());
    }

    private boolean isEmptyOrNull(String str) {
        return str == null || str.trim().isEmpty();
    }
}
