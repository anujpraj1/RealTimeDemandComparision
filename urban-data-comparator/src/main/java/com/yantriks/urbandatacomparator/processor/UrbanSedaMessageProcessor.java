package com.yantriks.urbandatacomparator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yantriks.urbandatacomparator.model.UrbanCsvData;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class UrbanSedaMessageProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        List<String> inputList = exchange.getIn().getBody(List.class);
        System.out.println("My String :: "+inputList);
        log.debug("UrbanSedaMessageProcessor: Input to UrbanSedaMessageProcessor"+exchange.getIn().getBody());
        log.debug("UrbanSedaMessageProcessor: Current Thread : "+Thread.currentThread());
        UrbanCsvData urbanCsvData = new UrbanCsvData();
        urbanCsvData.setReservationId(inputList.get(0).trim());
        urbanCsvData.setEnterpriseCode(inputList.get(1).trim());
        urbanCsvData.setOrderId(inputList.get(2).trim());
        log.debug("UrbanSedaMessageProcessor: Sending the Data : "+urbanCsvData.toString());
        exchange.getIn().setBody(urbanCsvData);
    }
}
