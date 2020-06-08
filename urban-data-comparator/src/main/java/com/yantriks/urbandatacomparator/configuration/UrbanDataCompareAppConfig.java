package com.yantriks.urbandatacomparator.configuration;

import com.yantriks.urbandatacomparator.route.UrbanCSVRoute;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetInvListCall;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetOrderListCall;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultShutdownStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class UrbanDataCompareAppConfig {

    //@Autowired
   // private CamelContext camelContext;


    @Value("${graceful.shutdown.timeoutinseconds}")
    private long gracefulShutdownTimeoutInSeconds;

    @Autowired
    UrbanCSVRoute urbanCsvRoute;


    @PostConstruct
    public void postConstruct() throws Exception {

        CamelContext camelContext = new DefaultCamelContext();
        log.info("UrbanDataCompareAppConfig, Staring Camel Context adding Routes");
        camelContext.addRoutes(urbanCsvRoute);
        camelContext.start();
        //Thread.sleep(300000);
        //camelContext.stop();
    }

}

