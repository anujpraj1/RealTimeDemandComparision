package com.yantriks.urbandatacomparator.configuration;

import com.yantriks.urbandatacomparator.route.UrbanCSVRoute;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetInvListCall;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetOrderListCall;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class UrbanDataCompareAppConfig {

    //@Autowired
   // private CamelContext camelContext;

    @Autowired
    UrbanCSVRoute urbanCsvRoute;

    @PostConstruct
    public void postConstruct() throws Exception {

        CamelContext camelContext = new DefaultCamelContext();
        System.out.println("Here in Post Construct");
        camelContext.addRoutes(urbanCsvRoute);
        camelContext.start();
        Thread.sleep(4000);
        camelContext.stop();
        //sterlingGetInvListCall.executeGetInvListApi("getExternalInvCheck_internal");
        //sterlingGetOrderListCall.executeGetOLListApi("Y100005200", "DEFAULT");
    }

}
