package com.yantriks.urbandatacomparator.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class UrbanDataCompareProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        String myString = exchange.getIn().getBody(String.class);
        System.out.println("Here is the String :: "+myString+"+And Thread :: "+Thread.currentThread());
        exchange.getIn().setBody(myString);
    }
}
