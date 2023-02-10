package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.DeadLetterQueueEvent;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DeadLetterQueueProcessor implements TransactionCheck {

  @Autowired
  StreamBridge sb;

  @Autowired
  IngestorRepository repository;

  private int processedTrx = 0;
  private int exceptionTrx = 0;

  @Override
  public void transactionCheckProcess(Stream<Transaction> readTransaction) {
    processedTrx = 0;
    exceptionTrx = 0;
    readTransaction.forEach(t -> {
      try {
        Optional<EPIItem> dbResponse = repository.findItemByHash(t.getHpan());
        if (dbResponse.isPresent()) {
          t.setHpan(dbResponse.get().getHashPan());
          sb.send("rtdTrxProducer-out-0", MessageBuilder.withPayload(t).build());
          processedTrx++;
        }
      } catch (Exception ex) {
        DeadLetterQueueEvent dlqException = new DeadLetterQueueEvent(t, ex.getMessage());
        sb.send("rtdDlqTrxProducer-out-0", MessageBuilder.withPayload(dlqException).build());
        log.error("[DLQ] Error getting records : {}", ex.getMessage());
        exceptionTrx++;
      }
    });
  }

  protected int getProcessedTrx() {
    return processedTrx;
  }

  protected int getExcepitonTrx() {
    return exceptionTrx;
  }
}
