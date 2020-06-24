package com.yantriks.urbandatacomparator.util;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSException;
import com.yantriks.urbandatacomparator.model.UrbanCsvOutputData;
import com.yantriks.urbandatacomparator.model.UrbanURI;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingAPIDocumentCreator;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingAPIUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class YantriksUtil {

    @Value("${urban.yantriks.timeout}")
    private Integer timeout;

    @Autowired
    UrbanURI urbanURI;

    @Autowired
    SterlingAPIDocumentCreator sterlingAPIDocumentCreator;

    @Autowired
    SterlingAPIUtil sterlingAPIUtil;


    public String callYantriksGetOrDeleteAPI(String apiUrl, String httpMethod, String productToCall) {
        //log.beginTimer("callYantriksGetOrDeleteAPI");
        log.debug("YantriksUtil: callYantriksGetOrDeleteAPI API :URL for Get or Delete :: "+apiUrl);
        log.debug("YantriksUtil: callYantriksGetOrDeleteAPI : Http Method :: "+httpMethod);
        if ((YFCCommon.isVoid(httpMethod)) || (YFCCommon.isVoid(apiUrl))) {
            if (log.isDebugEnabled())
                log.debug("Mandatory parameters are missing");
            if (log.isDebugEnabled()) {
                log.debug("httpMethod:: " + httpMethod + "apiUrl:: " + apiUrl);
            }
            return "";
        }
        String outputStr = "";

        URL url = null;
        HttpURLConnection conn = null;
        try {
            switch(productToCall) {
                case UrbanConstants.V_PRODUCT_YAS:
                    url = new URL(urbanURI.getAvailabilityURL(apiUrl));
                    break;
                case UrbanConstants.V_PRODUCT_ILT:
                    url = new URL(urbanURI.getInvLiteURL(apiUrl));
                    break;
                case UrbanConstants.V_PRODUCT_YCS:
                    url = new URL(urbanURI.getCommonURL(apiUrl));
                    break;
                default:
                    log.debug("YantriksUtil:Defaulting the URL to availability URL");
                    url = new URL(urbanURI.getAvailabilityURL(apiUrl));
            }


            if (log.isDebugEnabled())
                log.debug("YantriksUtil: callYantriksGetOrDeleteAPI: URL is:" + url.toString());


            long startTime = System.currentTimeMillis();

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod(httpMethod);
            String strJWTToken = GenerateSignedJWTToken.getJWTTokenStr();
            log.debug("JWT TOKEN :: "+strJWTToken);
            conn.setRequestProperty("Authorization", "Bearer  " + strJWTToken);

            log.debug("callYantriksGetOrDeleteAPI : TimeOut Value : "+timeout);
            if (!YFCCommon.isVoid(timeout)) {
                conn.setConnectTimeout(timeout);
            }
            long endTime = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("Output from Server ...." + conn.toString());
            }
            log.debug("Response Code Received :: "+conn.getResponseCode());
            if (conn.getResponseCode() != 200 || conn.getResponseCode() != 201) {
                log.info("We have not received response code as 200 or 201 hence will return the output from errorStream");
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));

                String outputLine = null;
                while ((outputLine = br.readLine()) != null) {
                    outputStr = outputStr.concat(outputLine);
                }
                log.debug("Output callYantriksGetOrDeleteAPI :: "+outputStr);
                return "FAILURE";
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String outputLine = null;
            while ((outputLine = br.readLine()) != null) {
                outputStr = outputStr.concat(outputLine);
            }
            if (log.isDebugEnabled()) {
                log.debug("Output from Server ::");
                log.debug(outputStr);
            }
            conn.disconnect();
        } catch (Exception e) {
            log.error("Error : " + e.getMessage() + " URL: " + apiUrl + " for Method :: " + httpMethod);
            throw new YFSException("Exception is thrown from yantriks API :: " + e.getMessage());
        } finally {
            log.debug("Finally Closing Connection");
            conn.disconnect();
        }
        return outputStr;
    }


    public String callYantriksAPI(String apiUrl, String httpMethod, String body, String productToCall) {
        //log.beginTimer("callYantriksGetOrDeleteAPI");
        log.debug("YantriksUtil: callYantriksAPI API :URL for call yantriks API :: "+apiUrl);
        log.debug("YantriksUtil: callYantriksAPI : Http Method :: "+httpMethod);
        if ((YFCCommon.isVoid(httpMethod)) || (YFCCommon.isVoid(apiUrl))) {
            if (log.isDebugEnabled())
                log.debug("Mandatory parameters are missing");
            if (log.isDebugEnabled()) {
                log.debug("httpMethod:: " + httpMethod + "apiUrl:: " + apiUrl);
            }
            return "";
        }
        String outputStr = "";

        URL url = null;
        HttpURLConnection conn = null;
        try {
            switch(productToCall) {
                case UrbanConstants.V_PRODUCT_YAS:
                    url = new URL(urbanURI.getAvailabilityURL(apiUrl));
                    break;
                case UrbanConstants.V_PRODUCT_ILT:
                    url = new URL(urbanURI.getInvLiteURL(apiUrl));
                    break;
                case UrbanConstants.V_PRODUCT_YCS:
                    url = new URL(urbanURI.getCommonURL(apiUrl));
                    break;
                default:
                    log.debug("YantriksUtil:Defaulting the URL to availability URL");
                    url = new URL(urbanURI.getAvailabilityURL(apiUrl));
            }


            if (log.isDebugEnabled())
                log.debug("YantriksUtil: callYantriksAPI: URL is:" + url.toString());


            long startTime = System.currentTimeMillis();

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod(httpMethod);
            String strJWTToken = GenerateSignedJWTToken.getJWTTokenStr();
            log.debug("JWT TOKEN :: "+strJWTToken);
            conn.setRequestProperty("Authorization", "Bearer  " + strJWTToken);

            log.debug("callYantriksAPI : TimeOut Value : "+timeout);

            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes());
            os.flush();

            if (!YFCCommon.isVoid(timeout)) {
                conn.setConnectTimeout(timeout);
            }
            long endTime = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("Output from Server ...." + conn.toString());
            }
            log.debug("Response Code Received :: "+conn.getResponseCode());
            if (conn.getResponseCode() != 200 || conn.getResponseCode() !=201) {
                log.info("We have not received response code as 200 or 201 hence will return the output from errorStream");
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));

                String outputLine = null;
                while ((outputLine = br.readLine()) != null) {
                    outputStr = outputStr.concat(outputLine);
                }
                log.debug("Output String returned from Server :: "+outputStr);
                return outputStr;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String outputLine = null;
            while ((outputLine = br.readLine()) != null) {
                outputStr = outputStr.concat(outputLine);
            }
            if (log.isDebugEnabled()) {
                log.debug("Output from Server::");
                log.debug(outputStr);
            }
            conn.disconnect();
        } catch (Exception e) {
            log.error("Error : " + e.getMessage() + " URL: " + apiUrl + " for Method :: " + httpMethod);
            throw new YFSException("Exception is thrown from yantriks API :: " + e.getMessage());
        } finally {
            log.debug("Finally Closing Connection");
            conn.disconnect();
        }
        return outputStr;
    }

    public String getLocationType(String locationId) throws Exception {
        Document getShipNodeInDoc = sterlingAPIDocumentCreator.createInDocForGetShipNodeList(locationId);
        Document shipNodeList = sterlingAPIUtil.invokeSterlingAPI(getShipNodeInDoc, SCXmlUtil.createFromString(UrbanConstants.TEMPLATE_GET_SHIPNODE_LIST), UrbanConstants.API_GET_SHIP_NODE_LIST);
        //Document orgList = sterlingGetOrganizationListCall.executeGetOrganizationListCall(locationId);
        //Element eleOrganization = SCXmlUtil.getChildElement(shipNodeList.getDocumentElement(), UrbanConstants.ELE_SHIPNODE);
        Element eleShipNode = SCXmlUtil.getChildElement(shipNodeList.getDocumentElement(), UrbanConstants.A_SHIP_NODE);
        if (!YFCObject.isVoid(eleShipNode)) {
            String nodeType = eleShipNode.getAttribute(UrbanConstants.NODE_TYPE);
            Element extnNode = SCXmlUtil.getChildElement(eleShipNode, UrbanConstants.ELE_EXTN);
            String nodeClass = "";
            if (!YFCObject.isVoid(extnNode)) {
                nodeClass = extnNode.getAttribute(UrbanConstants.E_NODE_CLASS);
            }
            if (UrbanConstants.V_STORE.equalsIgnoreCase(nodeType)) {
                return UrbanConstants.V_STORE;
            } else if (!YFCObject.isVoid(extnNode) && UrbanConstants.V_AN.equalsIgnoreCase(nodeClass)) {
                return UrbanConstants.LT_AGG_NODES;
            } else if (UrbanConstants.VENDOR_LIST.contains(nodeType)) {
                return UrbanConstants.LT_VENDOR;
            } else if (UrbanConstants.V_MIRAKL_SHIP_NODE.contains(nodeType) || UrbanConstants.LT_MSN.contains(nodeType)) {
                return UrbanConstants.LT_MSN;
            } else if (UrbanConstants.LT_BACK_OFFICE.equalsIgnoreCase(nodeType)) {
                return UrbanConstants.LT_BACK_OFFICE;
            } else if (UrbanConstants.VAL_DC.equalsIgnoreCase(nodeType) && !YFCObject.isVoid(extnNode)) {
                return UrbanConstants.LT_DC;
            } else if (UrbanConstants.V_CALL_CENTRE.equalsIgnoreCase(nodeType) && !YFCObject.isVoid(extnNode)
                    && (UrbanConstants.V_CALL_CENTRE.equalsIgnoreCase(nodeClass) || UrbanConstants.V_GC.equalsIgnoreCase(nodeClass))) {
                return UrbanConstants.LT_CC;
            } else {
                log.debug("YantriksCommonUtil :: getLocationType ::Valid Location Type not found hence returning null");
                return null;
            }
        }
        return null;
    }

    public String getCurrentDateOrTimeStamp(SimpleDateFormat formatter) {
       /* String timeZone = YFSSystem.getProperty(YantriksConstants.PROP_TIME_ZONE);
        if (timeZone.equals("")) {
            timeZone = "UTC"; //Setting UTC as default
        }*/
        formatter.setTimeZone(TimeZone.getTimeZone(UrbanConstants.V_TZ_UTC));
        Date date = new Date();
        return formatter.format(date);
    }

    public List<String> calculateExpirationTimeAndDate(String expirationDate) throws ParseException {

        List<String> listToReturn = new ArrayList<>();
        int expirationTime = 0; // Default
        String expirationTimeUnit = UrbanConstants.V_SECONDS; // Default
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        formatter.setTimeZone(TimeZone.getTimeZone(UrbanConstants.V_TZ_UTC));
        Date expDate = formatter.parse(expirationDate);
        String strCurrentDate = formatter.format(new Date());
        Date currentDate = formatter.parse(strCurrentDate);
        long diffInMillies = Math.abs(expDate.getTime() - currentDate.getTime());
        long diffInHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        if (diffInHours == 0) {
            long diffInMinutes = TimeUnit.MINUTES.convert(diffInMillies, TimeUnit.MILLISECONDS);
            if (diffInMinutes == 0) {
                long diffInSeconds = TimeUnit.SECONDS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                expirationTime = Integer.parseInt(String.valueOf(diffInSeconds));
            } else {
                expirationTime = Integer.parseInt(String.valueOf(diffInMinutes));
                expirationTimeUnit = "MINUTES";
            }
        } else {
            expirationTime = Integer.parseInt(String.valueOf(diffInHours));
            expirationTimeUnit = "HOURS";
        }
        log.debug("calculateExpirationTimeAndDate: Setting Expiration Time :: " + expirationTime + "Expiration Time Unit :: " + expirationTimeUnit);
        listToReturn.add(String.valueOf(expirationTime));
        listToReturn.add(expirationTimeUnit);
        return listToReturn;
    }

    public String getReservationID(Element ele) {
        Element childEle = SCXmlUtil.getChildElement(ele, UrbanConstants.ELE_EXTN);
        if (!YFCObject.isVoid(childEle)) {
            if (!YFCObject.isVoid(childEle.getAttribute(UrbanConstants.EXTN_RESERVATION_ID))) {
                return childEle.getAttribute(UrbanConstants.EXTN_RESERVATION_ID);
            } else {
                return ele.getAttribute(UrbanConstants.A_ORDER_NO);
            }
        } else {
            return ele.getAttribute(UrbanConstants.A_ORDER_NO);
        }
    }

    public void setCSVOutput(UrbanCsvOutputData outputData, String extnReserveId,
                             String orderId, String enterpriseCode, boolean isCompareAndGenerate,
                             String reservationStatus, int reservationResponseCode, String error, String message) {

    }

    public String determineErrorOrSuccessOnReservationPost(String reservationRestCallOutput) throws JSONException {
        if (reservationRestCallOutput.equals("")) {
            return UrbanConstants.V_FAILURE;
        } else if (UrbanConstants.V_EXC_FAILURE.equals(reservationRestCallOutput)) {
            return UrbanConstants.V_EXC_FAILURE;
        } else {
            JSONObject outputObj = new JSONObject(reservationRestCallOutput);
            log.debug("Output Object :: "+outputObj.toString());
            log.debug("Status Check : "+outputObj.containsKey("status"));
            if (!outputObj.containsKey("status")) {
                return "SUCCESS";
            } else {
                return UrbanConstants.V_FAILURE;
            }
        }
    }

    public void defaultIncorrectDataToPopulate(StringBuilder csvWriteData, String reservationId, String enterpriseCode, String orderId, String errorResponse) {
        csvWriteData.append(reservationId);
        csvWriteData.append("|");
        csvWriteData.append(enterpriseCode);
        csvWriteData.append("|");
        csvWriteData.append(orderId);
        csvWriteData.append("|");
        csvWriteData.append(errorResponse);
    }

    public void dataFromCompareAndGenerate(StringBuilder csvWriteData, UrbanCsvOutputData urbanCsvData) {
        csvWriteData.append(urbanCsvData.getExtnReservationId());
        csvWriteData.append("|");
        csvWriteData.append(urbanCsvData.getEnterpriseCode());
        csvWriteData.append("|");
        csvWriteData.append(urbanCsvData.getOrderId());
        csvWriteData.append("|");
        csvWriteData.append(urbanCsvData.getReservationStatus());
    }

    public void dataFromCompareAndUpdate(StringBuilder csvWriteData, UrbanCsvOutputData urbanCsvData) {
        csvWriteData.append(urbanCsvData.getExtnReservationId());
        csvWriteData.append("|");
        csvWriteData.append(urbanCsvData.getEnterpriseCode());
        csvWriteData.append("|");
        csvWriteData.append(urbanCsvData.getOrderId());
        csvWriteData.append("|");
        csvWriteData.append(urbanCsvData.getReservationResponseCode());
        csvWriteData.append("|");
        csvWriteData.append(urbanCsvData.getError());
        csvWriteData.append("|");
        csvWriteData.append(urbanCsvData.getMessage());
    }

    public void populateCSVData(StringBuilder csvWriteData, UrbanCsvOutputData urbanCsvOutputData) {
        if (urbanCsvOutputData.isCompareAndGenerate()) {
            log.info("YantriksUtil : Only Data will be generated no update");
            dataFromCompareAndGenerate(csvWriteData, urbanCsvOutputData);
        } else {
            log.info("YantriksUtil: Compare And Update Data");
            dataFromCompareAndUpdate(csvWriteData, urbanCsvOutputData);
        }
    }

    /**
     *
     * @param strDate
     * @return
     * @throws ParseException
     */
    public String getDateinUTC(String strDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        Date date1= sdf.parse(strDate);
        log.debug("shipDate "+strDate);
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        TimeZone tz = TimeZone.getTimeZone("UTC");
        sdf1.setTimeZone(tz);
        String formattedDate = (sdf1.format(date1));
        return  formattedDate;
    }

    /**
     *
     * @param filePath
     * @return
     */
    public static String getJSONFromFile(String filePath) {

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {

            // read line by line
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }
        return sb.toString();
    }
}
