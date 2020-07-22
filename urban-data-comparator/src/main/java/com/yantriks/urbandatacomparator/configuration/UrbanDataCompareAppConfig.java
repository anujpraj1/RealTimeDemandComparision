package com.yantriks.urbandatacomparator.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yantriks.urbandatacomparator.route.UrbanCSVRoute;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetInvListCall;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetOrderListCall;
import com.yantriks.urbandatacomparator.util.GenerateSignedJWTToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.net.ssl.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class UrbanDataCompareAppConfig {

    //@Autowired
   // private CamelContext camelContext;

    public static String INSTANCE_ID = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));


    @Value("${graceful.shutdown.timeoutinseconds}")
    private long gracefulShutdownTimeoutInSeconds;

    @Value("${data.output.filename}")
    private String outFileName;

    @Value("${data.output.absolutedirectory}")
    private String absOPDirectoryPath;



    @Autowired
    UrbanCSVRoute urbanCsvRoute;

    @Autowired
    CamelContext camelContext;

    @PostConstruct
    public void postConstruct() throws Exception {

//        deleteContentOfOutputFile();
        startCamelContext();
    }

    static {
        disableSslVerification();
    }

    private static void disableSslVerification() {
        try
        {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }
    private void deleteContentOfOutputFile() throws IOException, InterruptedException {
        String fileName = absOPDirectoryPath+File.separator+INSTANCE_ID+File.separator+outFileName;
        log.info("File with path : "+fileName);
        File file = new File(fileName);
        PrintWriter writer = new PrintWriter(file);
        writer.print("");
        writer.println("EXTN_RESERVATIONID|ENTERPRISE_CODE|ORDERID|RESPONSE_CODE/ERROR_RESPONSE|ERROR|MESSAGE");
        writer.flush();
        writer.close();
    }

    private void startCamelContext() throws Exception {
//        CamelContext camelContext = new DefaultCamelContext();
        log.info("UrbanDataCompareAppConfig, Staring Camel Context adding Routes");
        camelContext.addRoutes(urbanCsvRoute);
        camelContext.start();

//        Thread.sleep(1800000);
//        camelContext.stop();
    }


    @Bean
    public ObjectMapper objectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;

    }


}

