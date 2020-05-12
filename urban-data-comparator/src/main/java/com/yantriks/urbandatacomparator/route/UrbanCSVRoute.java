package com.yantriks.urbandatacomparator.route;

import com.yantriks.urbandatacomparator.processor.UrbanSedaMessageProcessor;
import com.yantriks.urbandatacomparator.processor.UrbanDataCompareProcessor;
import com.yantriks.urbandatacomparator.validation.UrbanConditionCheck;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
@Slf4j
public class UrbanCSVRoute extends RouteBuilder {

    private static String SEDA_END_POINT = "seda:datacomparequeue";

    @Value("${data.mode.compareAndUpdate}")
    private Boolean actionMode;

    @Value("${data.upload.input.skipHeader}")
    private Boolean skipHeader;

    @Autowired
    UrbanConditionCheck urbanConditionCheck;

    @Override
    public void configure() throws Exception {

        onCompletion().process(exchange -> {
            System.out.println("Success" + exchange.getIn().getBody());
        });
        onException().process(exchange -> {
            System.out.println("Error is :" + exchange.getIn().getBody());
        });

        CsvDataFormat csvDataFormat = new CsvDataFormat();
        csvDataFormat.setLazyLoad(true);
        csvDataFormat.setSkipHeaderRecord(skipHeader);
        System.out.println("DATA MODE :: "+actionMode);
        urbanConditionCheck.setActionMode(actionMode);

        log.info("Data Mode Log Level :: "+actionMode);
        log.debug("Data Mode Log Level :: "+actionMode);
        log.error("Error mode");
        log.warn("Error mode Warnign");


        from("file:/home/YANTRIKS/anuj.kumar/YantriksStuff/csvdir/input?noop=true&maxMessagesPerPoll=1&delay=5000")
                .unmarshal(csvDataFormat)
                .split(body())
                .streaming()
                .executorService(Executors.newFixedThreadPool(3))
                //.to("file:/home/YANTRIKS/anuj.kumar/YantriksStuff/csvdir/output");
                .choice()
                .when(method(urbanConditionCheck, "isActionModeCompareAndUpdate"))
                .process(new UrbanDataCompareProcessor())
                //.transform(body().append("\n"))
                .to(SEDA_END_POINT)
                .otherwise()
                .process(new UrbanDataCompareProcessor())
                .to("file:/home/YANTRIKS/anuj.kumar/YantriksStuff/csvdir/output?fileExist=Append")
                .end();

        from(SEDA_END_POINT).threads(5).process(new UrbanSedaMessageProcessor()).to("file:/home/YANTRIKS/anuj.kumar/YantriksStuff/csvdir/output?fileExist=Append");
    }
}
