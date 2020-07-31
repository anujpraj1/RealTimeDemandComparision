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

import java.io.File;
import java.util.concurrent.Executors;

import static com.yantriks.urbandatacomparator.configuration.UrbanDataCompareAppConfig.INSTANCE_ID;

@Component
@Slf4j
public class UrbanCSVRoute extends RouteBuilder {

    @Value("${data.mode.comparegenerate}")
    private Boolean actionMode;

    @Value("${data.upload.input.skipHeader}")
    private Boolean skipHeader;

    @Value("${data.process.sedathreads}")
    private Integer sedathreads;

    @Value("${data.process.csvthreads}")
    private Integer csvthreads;

    @Value("${data.input.absolutedirectory}")
    private String absInDirectoryPath;

    @Value("${data.output.absolutedirectory}")
    private String absOPDirectoryPath;

    @Value("${data.input.filename}")
    private String inFileName;

    @Value("${data.output.filename}")
    private String outFileName;

    @Value("${data.input.option.noop}")
    private String optNoop;

    @Value("${data.input.option.maxMessagesPerPoll}")
    private String optMaxMessagesPerPoll;

    @Value("${data.input.option.delay}")
    private String optDelay;

    @Value("${data.output.option.fileexists}")
    private String optFileExists;

    @Value("${seda.queue}")
    private String sedaQueue;

    @Value("${seda.queueSize}")
    private String sedaQueueSize;

    @Autowired
    UrbanConditionCheck urbanConditionCheck;

    @Autowired
    UrbanDataCompareProcessor urbanDataCompareProcessor;

    @Autowired
    UrbanSedaMessageProcessor urbanSedaMessageProcessor;

    @Override
    public void configure() throws Exception {

        String inputFileURI = getAbsFileURIPath(absInDirectoryPath) + getInputQueryParams();
        log.debug("Input FIle URI :: " + inputFileURI);
        String outputFileURI = getAbsFileURIPath(absOPDirectoryPath+INSTANCE_ID+ File.separator)
                + getOutputQueryParams();
        log.debug("Output FIle URI :: " + outputFileURI);

        String SEDA_END_POINT = getSedaUri(sedaQueue);
        //uri="seda:message?size=16&amp;blockWhenFull=true" />

        onCompletion().process(exchange -> {
            log.debug("Success Response for " + exchange.getIn().getBody());
        });
        onException().process(exchange -> {
            log.error("Error Received for " + exchange.getIn().getBody());
        });

        CsvDataFormat csvDataFormat = new CsvDataFormat();
        csvDataFormat.setLazyLoad(true);
        csvDataFormat.setSkipHeaderRecord(skipHeader);

        log.debug("Here First Even before going to routes");

        //String fileUri = absInDirectoryPath+"?fileName="+inFileName+"&noop"+optNoop+"&maxMessagesPerPoll="+optMaxMessagesPerPoll+"&delay="+optDelay;

        from(inputFileURI)
                .unmarshal(csvDataFormat)
                .split(body())
                .streaming()
                .executorService(Executors.newFixedThreadPool(csvthreads))
                .process(urbanSedaMessageProcessor)
                .to(SEDA_END_POINT);

        from(SEDA_END_POINT).threads(sedathreads).process(urbanDataCompareProcessor)
                .transform(body().append("\n"))
                .log("Processed Record ${body}")
                .to(outputFileURI);

    }

    private String getInputQueryParams() {
        StringBuilder sbFileUri = new StringBuilder();
        sbFileUri.append("?fileName=");
        sbFileUri.append(inFileName);
        sbFileUri.append("&noop=");
        sbFileUri.append(optNoop);
        sbFileUri.append("&maxMessagesPerPoll=");
        sbFileUri.append(optMaxMessagesPerPoll);
        sbFileUri.append("&delay=");
        sbFileUri.append(optDelay);

        return sbFileUri.toString();
    }

    private String getOutputQueryParams() {
        StringBuilder sbFileUri = new StringBuilder();
        sbFileUri.append("?fileName=");
        sbFileUri.append(outFileName);
        sbFileUri.append("&fileExist=");
        sbFileUri.append(optFileExists);

        return sbFileUri.toString();
    }

    private String getAbsFileURIPath(String absDirectoryPath) {
        return "file:" + absDirectoryPath;
    }

    private String getSedaUri(String sedaQueue) {
        return "seda:" + sedaQueue+"?size="+sedaQueueSize;
//        uri="seda:message?size=16&amp;blockWhenFull=true"
    }

}
