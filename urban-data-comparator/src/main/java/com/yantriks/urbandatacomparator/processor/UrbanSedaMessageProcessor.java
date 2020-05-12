package com.yantriks.urbandatacomparator.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class UrbanSedaMessageProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        String myString = exchange.getIn().getBody(String.class);
        System.out.println("Body inside Seda Processor"+exchange.getIn().getBody());
        System.out.println("Thread Value :: "+Thread.currentThread());
        exchange.getIn().setBody(myString);
    }
}
