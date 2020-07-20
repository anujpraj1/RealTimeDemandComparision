package com.yantriks.urbandatacomparator.configuration;

import com.yantriks.urbandatacomparator.route.UrbanCSVRoute;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetInvListCall;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetOrderListCall;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class UrbanDataCompareAppConfig {

    //@Autowired
   // private CamelContext camelContext;


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

        deleteContentOfOutputFile();
        startCamelContext();
    }

    private void deleteContentOfOutputFile() throws IOException, InterruptedException {
        String fileName = absOPDirectoryPath+"\\"+outFileName;
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

}

