package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yantriks.urbandatacomparator.model.UrbanCsvData;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UrbanSedaMessageProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        UrbanCsvData myString = exchange.getIn().getBody(UrbanCsvData.class);
        System.out.println("My String :: "+myString);
        log.debug("UrbanSedaMessageProcessor: Input to UrbanSedaMessageProcessor"+exchange.getIn().getBody());
        log.debug("UrbanSedaMessageProcessor: Current Thread : "+Thread.currentThread());
        String h = ",,getExternalInvCheck_internal";
        String[] currentData = h.split(",");
        System.out.println("Current Data :: "+currentData);
        UrbanCsvData urbanCsvData = new UrbanCsvData();
        urbanCsvData.setOrderId(currentData[0].trim());
        urbanCsvData.setEnterpriseCode(currentData[1].trim());
        urbanCsvData.setReservationId(currentData[2].trim());
        log.debug("UrbanSedaMessageProcessor: Sending the Data : "+urbanCsvData.toString());
        exchange.getIn().setBody(urbanCsvData);
    }
}
