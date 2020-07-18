package com.yantriks.urbandatacomparator.util;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSException;
import com.yantriks.urbandatacomparator.model.UrbanCsvOutputData;
import com.yantriks.urbandatacomparator.model.UrbanURI;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingAPIDocumentCreator;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingAPIUtil;
import com.yantriks.yih.adapter.util.YantriksCommonUtil;
import com.yantriks.yih.adapter.util.YantriksConstants;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
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

    @Value("${security.secretkey}")
    private String strSecretKey;

    @Value("${security.skid}")
    private String strSkid;

    @Value("${security.expireTime}")
    private String strExpirytime;

    private String strSecurityKey="da53d169065e21d726190c529d2c28f6a3b41ded45b5b382c4c23d139faebe95";

    @Autowired
    UrbanURI urbanURI;

    @Autowired
    SterlingAPIDocumentCreator sterlingAPIDocumentCreator;

    @Autowired
    SterlingAPIUtil sterlingAPIUtil;


    public String callYantriksGetOrDeleteAPI(String apiUrl, String httpMethod, String productToCall) throws YIFClientCreationException, ParserConfigurationException {
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
//        boolean isHttpMethodDelete = false;
//        if("DELETE".equalsIgnoreCase(httpMethod)){
//            isHttpMethodDelete = true;
//        }

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

                log.debug("availability URL "+url.toString());
            if (log.isDebugEnabled())
                log.debug("YantriksUtil: callYantriksGetOrDeleteAPI: URL is:" + url.toString());


            long startTime = System.currentTimeMillis();
            /************/
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("nyproxy.urbanout.com", 8080));

            conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod(httpMethod);
            conn.setRequestProperty("Content-Type","application/json");
            String strJWTToken = GenerateSignedJWTToken.getJWTTokenStr(strSecretKey,strSkid,strExpirytime);
            log.debug("JWT TOKEN :: "+strJWTToken);
            conn.setRequestProperty("Authorization", "Bearer  " + strJWTToken);

            log.debug("callYantriksGetOrDeleteAPI : TimeOut Value : "+timeout);
            if (!YFCCommon.isVoid(timeout)) {
                conn.setConnectTimeout(timeout);
            }
            long endTime = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("Output from Server ...." + conn.getResponseMessage().toString());
            }
            log.debug("Response Code Received :: "+conn.getResponseCode());
            if (conn.getResponseCode() != 200 && conn.getResponseCode() != 201 && conn.getResponseCode()!=204) {
                log.info("We have not received response code as 200 or 201 hence will return the output from errorStream");
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));

                String outputLine = null;
                while ((outputLine = br.readLine()) != null) {
                    outputStr = outputStr.concat(outputLine);
                }
                log.debug("Output callYantriksGetOrDeleteAPI :: "+outputStr);
                return "FAILURE";
            }
             if(conn.getResponseCode()==204){
                log.info("No content or record found in yantriks");
                log.info("Hence directUpdate needs to be done to yantriks , returning \"\" ");
                return "";
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
//            throw e;
        } finally {
            log.debug("Finally Closing Connection");
            conn.disconnect();
        }
        return outputStr;
    }
// just added

        public String callYantriksAPI(String apiUrl, String httpMethod, String body, String productToCall) throws YIFClientCreationException, ParserConfigurationException, IOException {
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
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("nyproxy.urbanout.com", 8080));

            conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod(httpMethod);
            conn.setRequestProperty("Content-Type","application/json");
            String strJWTToken = GenerateSignedJWTToken.getJWTTokenStr(strSecretKey,strSkid,strExpirytime);
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
            if (conn.getResponseCode() != 200 && conn.getResponseCode() !=201
                    && conn.getResponseCode()!=204 && conn.getResponseCode()!=400) {
                log.info("We have not received response code as 200 or 201 hence will return the output from errorStream");
                log.debug("Output String returned from Server :: "+outputStr);
                return "FAILURE";
            }

            if(conn.getResponseCode()==204){
                log.info("No content or record found in yantriks");
                log.info("Hence directUpdate needs to be done to yantriks , returning \"\" ");
                return "";
            }

            if(conn.getResponseCode()==400){
                log.info("Statud received "+conn.getResponseCode());
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String outputLine = null;
                while ((outputLine = br.readLine()) != null) {
                    outputStr = outputStr.concat(outputLine);
                }
                if(outputStr.contains("NOT_ENOUGH_ATP")){
                    log.debug("NOT_ENOUGH_ATP");
                    return "NOT_ENOUGH_ATP";
                }
                else{
                    return "FAILURE";
                }

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
            //throw e;
        } finally {
            log.debug("Finally Closing Connection");
            conn.disconnect();
        }
        return outputStr;
    }

    /***
     *
     * @param apiUrl
     * @param httpMethod
     * @param body
     * @param productToCall
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public String callYantriksAPIV3(String apiUrl, String httpMethod,  String body,  String productToCall)
            throws URISyntaxException, IOException {

//        log.beginTimer("YantriksCommonUtil.callYantrikAPI");
        long beginMS = System.currentTimeMillis();
        log.debug("Input to method");
        log.debug("apiUrl : "+apiUrl);
        log.debug("httpMethod : "+httpMethod);
        log.debug("productToCall : "+productToCall);
        log.debug("body : "+body);

        String protocol = YFSSystem
                .getProperty(YantriksConstants.YANTRIKSDOT + productToCall + YantriksConstants.DOTPROTOCOL);
        log.debug("protocol :"+protocol);
        String host = YFSSystem
                .getProperty(YantriksConstants.YANTRIKSDOT + productToCall + YantriksConstants.DOTHOSTNAME);
        log.debug("host :"+host);
        String port = YFSSystem.getProperty(YantriksConstants.YANTRIKSDOT + productToCall + YantriksConstants.DOTPORT);
        log.debug("port :"+port);
        String timeout = YFSSystem
                .getProperty(YantriksConstants.YANTRIKSDOT + productToCall + YantriksConstants.DOTTIMEOUT);
        log.debug("timeout :"+timeout);

        int iPort=0;
        if (!YFCCommon.isVoid(port)) {
            iPort = Integer.parseInt(port);
        }
        //setting uri properties
        URI uri = new URIBuilder()
                .setScheme(protocol)
                .setHost(host)
                .setPath(apiUrl)
                .setPort(iPort)
                .build();
        log.debug("url :"+uri.toURL());

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(protocol);
        urlBuilder.append("://");
        urlBuilder.append(host);
        if (!YFCObject.isVoid(port)) {
            urlBuilder.append(":");
            urlBuilder.append(port);
        }
        urlBuilder.append(apiUrl);

        log.debug("URL via String Builder : "+urlBuilder.toString());

        int intTimeOut = Integer.parseInt(timeout);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(intTimeOut)
                .setConnectTimeout(intTimeOut)
                .setSocketTimeout(intTimeOut)
                .build();

        final HttpPost httpPost = new HttpPost(urlBuilder.toString());
        httpPost.setEntity(new StringEntity(body));
        httpPost.setConfig(requestConfig);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE,"application/json");
        httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");
        log.debug(httpPost.toString());

        String strJWTToken = null;
        String strIsSecurityEnabled =  YFSSystem.getProperty(YantriksConstants.YIH_IS_API_SECURITY_ENABLED);
        log.debug(" strIsSecurityEnabled "+strIsSecurityEnabled);
        //creating a CloseableHttpClient Singleton object
        CloseableHttpClient closeableHttpClient = YantriksCloseableHttpClientSingleton.createCloseableHttpClient();
        String result = null;
        try {

            if (YantriksConstants.CONST_TRUE.equalsIgnoreCase(strIsSecurityEnabled)) {
                strJWTToken = GenerateSignedJWTToken.getJWTTokenStr(strSecretKey,strSkid,strExpirytime);
                log.debug("strJWTToken generated :"+strJWTToken);
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer"+ strJWTToken);
            }
            // invoking the api
            CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpPost);
            int iStatusCode = httpResponse.getStatusLine().getStatusCode();
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity);
            }
            log.debug("Response Code : "+iStatusCode);
            log.debug("Response received :"+result);

            if (iStatusCode == 200 || iStatusCode == 201) {
                log.debug("succesfull response received :"+iStatusCode);
                log.info("Response : " + result);
//                log.endTimer("YantriksCommonUtil.callYantrikAPI");
                return result;
            } else if (iStatusCode == 400) {
                JSONObject errObj = new JSONObject(result);
                String messageResponse = errObj.getString(UrbanConstants.JSON_ATTR_MESSAGE);
                String errorResponse = errObj.getString(UrbanConstants.JSON_ATTR_ERROR);
                log.debug("Message Response : "+messageResponse);
                log.debug("Error Response : "+errorResponse);
                if (UrbanConstants.IM_LIST_ENTITY_NOT_EXISTS.contains(messageResponse)) {
                    log.debug("Message Response is Entity does not exists hence will return this response");
//                    Urbanlog.writeToExitRest(YantriksCommonUtil.class,
//                            Thread.currentThread().getStackTrace()[1].getMethodName(), apiUrl,
//                            System.currentTimeMillis() - beginMS, iStatusCode, result, Level.INFO);
//                    log.endTimer("YantriksCommonUtil.callYantrikAPI");
                    return UrbanConstants.V_ENTITY_NOT_EXISTS;
                } else if (UrbanConstants.IM_LIST_GET_RESERVATION_FAILURES.contains(messageResponse) || UrbanConstants.IM_LIST_GET_RESERVATION_FAILURES.contains(errorResponse)) {
//                    log.debug("Either Message response is NOT_ENOUGH_ATP or error is Validation Error hence will return SYSTEM_EXCEPTION as output");
//                    Urbanlog.writeToExitRest(YantriksCommonUtil.class,
//                            Thread.currentThread().getStackTrace()[1].getMethodName(), apiUrl,
//                            System.currentTimeMillis() - beginMS, iStatusCode, result, Level.INFO);
//                    log.endTimer("YantriksCommonUtil.callYantrikAPI");
                    return "NON_RETRY_EXCEPTION";
                } else {
//                    Urbanlog.writeToExitRest(YantriksCommonUtil.class,
//                            Thread.currentThread().getStackTrace()[1].getMethodName(), apiUrl,
//                            System.currentTimeMillis() - beginMS, iStatusCode, result, Level.INFO);
//                    log.endTimer("YantriksCommonUtil.callYantrikAPI");
                    return YantriksConstants.V_FAILURE;
                }
            } else if (iStatusCode == 204) {//No Content found
                log.debug("No Content Found");
//                Urbanlog.writeToExitRest(YantriksCommonUtil.class,
//                        Thread.currentThread().getStackTrace()[1].getMethodName(), apiUrl,
//                        System.currentTimeMillis() - beginMS, iStatusCode, result, Level.INFO);
//                log.endTimer("YantriksCommonUtil.callYantrikAPI");
                return UrbanConstants.V_NO_CONTENT_FOUND;
            } else {
                log.debug("Returning Response as Failure");
//                Urbanlog.writeToExitRest(YantriksCommonUtil.class,
//                        Thread.currentThread().getStackTrace()[1].getMethodName(), apiUrl,
//                        System.currentTimeMillis() - beginMS, iStatusCode, result, Level.INFO);
//                log.endTimer("YantriksCommonUtil.callYantrikAPI");
                return YantriksConstants.V_FAILURE;
            }
        }
        catch(Exception exc) {
            log.error("Error :" + exc.getMessage() + " URL: " + apiUrl + " for Method :: " + httpMethod);
            throw new YFSException("Exception is thrown from yantriks API :: " + exc.getMessage());
        }
    }

    public String getLocationType(String locationId) throws Exception {
        Document getShipNodeInDoc = sterlingAPIDocumentCreator.createInDocForGetShipNodeList(locationId);
        Document shipNodeList = sterlingAPIUtil.invokeSterlingAPI(getShipNodeInDoc,
                SCXmlUtil.createFromString(UrbanConstants.TEMPLATE_GET_SHIPNODE_LIST),
                UrbanConstants.API_GET_SHIP_NODE_LIST);
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
        } else if(reservationRestCallOutput.equals(UrbanConstants.NOT_ENOUGH_ATP)){
            return UrbanConstants.NOT_ENOUGH_ATP;
        }
        else {
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
