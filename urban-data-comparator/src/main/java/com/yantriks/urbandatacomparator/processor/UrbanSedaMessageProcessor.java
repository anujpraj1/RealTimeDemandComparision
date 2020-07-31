package com.yantriks.urbandatacomparator.processor;

import com.yantriks.urbandatacomparator.model.UrbanCsvData;
import com.yantriks.urbandatacomparator.model.UrbanCsvOutputData;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class UrbanSedaMessageProcessor implements Processor {


    @Override
    public void process(Exchange exchange) throws Exception {
        List<String> inputList = exchange.getIn().getBody(List.class);
        log.debug("UrbanSedaMessageProcessor : Input List  :: "+inputList);
        log.debug("UrbanSedaMessageProcessor: Input to UrbanSedaMessageProcessor"+exchange.getIn().getBody());
        log.info("UrbanSedaMessageProcessor: Current Thread : "+Thread.currentThread());
        UrbanCsvData urbanCsvData = new UrbanCsvData();
//        try {
            urbanCsvData.setReservationId(inputList.get(0).trim());
            urbanCsvData.setEnterpriseCode(inputList.get(1).trim());
            urbanCsvData.setOrderId(inputList.get(2).trim());
            urbanCsvData.setDocumentType(inputList.get(3).trim());
            log.debug("UrbanSedaMessageProcessor: Sending the Data : " + urbanCsvData.toString());
            exchange.getIn().setBody(urbanCsvData);
//        }
//        catch (Exception exc){
//            log.debug("getMessage"+exc.getMessage());
//            int icomma =exc.getMessage().indexOf(",") ;
//            int icollen =exc.getMessage().indexOf(":") ;
//            log.debug("getMessage "+exc.getMessage().substring(icollen,icomma));
//            String strIndex = exc.getMessage().substring(icollen,icomma).trim();
//            if("1".equals(strIndex)){
//                urbanCsvOutputData.setEnterpriseCode(inputList.get(1).trim());
//            }
//            else if ("2".equals(strIndex)){
//                urbanCsvOutputData.setExtnReservationId(inputList.get(0).trim());
//                urbanCsvOutputData.setEnterpriseCode(inputList.get(1).trim());
//            }
//            else if("3".equals(strIndex)){
//                urbanCsvOutputData.setExtnReservationId(inputList.get(0).trim());
//                urbanCsvOutputData.setEnterpriseCode(inputList.get(1).trim());
//                urbanCsvOutputData.setExtnReservationId(inputList.get(2).trim());
//            }
//
//            urbanCsvOutputData.setError("DATA MISSING IN INPUT FILE");
//            urbanCsvOutputData.setMessage("PLEASE MAKE SURE ALL DATA IS PRESENT");
//        }
//        finally {
//            exchange.getIn().setBody(urbanCsvData);
//        }
    }
}
