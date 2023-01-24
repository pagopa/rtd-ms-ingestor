package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.mongodb.MongoException;

import lombok.extern.slf4j.Slf4j;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.DeadLetterQueueEvent;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


@Slf4j
@Service
public class DeadLetterQueueProcessor implements TransactionCheck{

    @Autowired
    StreamBridge sb;

    @Autowired
    IngestorRepository repository;

    private int processedTrx = 0;
    private int exceptionTrx = 0;

    @Override
    public void TransactionCheckProcess(Stream<Transaction> readTransaction) {
        processedTrx = 0;
        exceptionTrx = 0;
        readTransaction.forEach(t -> {
            try{
                TimeUnit.SECONDS.sleep(5);
                Optional<EPIItem> dbResponse = repository.findItemByHash(t.getHpan());
                if (dbResponse.isPresent()) {
                    t.setHpan(dbResponse.get().getHashPan());
                    sb.send("rtdTrxProducer-out-0", MessageBuilder.withPayload(t).build());
                    log.info(t.toString());
                    processedTrx++;
                }
            }catch(MongoException ex){
                DeadLetterQueueEvent edlq = new DeadLetterQueueEvent(t,ex.getMessage());
                sb.send("rtdDlqTrxProducer-out-0", MessageBuilder.withPayload(edlq).build());
                log.error("Error getting records : {}", ex.getMessage());
                exceptionTrx++;
            }catch(InterruptedException ie){
                log.error("Error setting sleeping time : {}", ie.getMessage());
                DeadLetterQueueEvent edlq = new DeadLetterQueueEvent(t,"Error setting sleeping time.");
                sb.send("rtdDlqTrxProducer-out-0", MessageBuilder.withPayload(edlq).build());
            }
        });
    }

    protected int getProcessedTrx(){
        return processedTrx;
    }

    protected int getExcepitonTrx(){
        return exceptionTrx;
    }
}
