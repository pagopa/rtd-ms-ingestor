package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * This class contains handling methods for blobs.
 */
@Service
@Slf4j
public class BlobRestConnector {

  @Autowired
  StreamBridge sb;

  public BlobApplicationAware download(BlobApplicationAware blob) {
    log.info("Init: {}", blob.getUri());
    return blob;
  }

  public BlobApplicationAware open(BlobApplicationAware blob) {
    log.info("Open: {}", blob.getUri());
    return blob;
  }

  /**
   * TEMPORARY IMPL - Method that currently set the Acquirer Id to "idtrx".
   *
   * @param blob the blob of the transaction.
   * @return the transaction with the Acquiredr id set to "idtrx".
   */
  public Transaction produce(BlobApplicationAware blob) {
    log.info("Produce: {}", blob.getUri());
    Transaction t = new Transaction();
    t.setIdTrxAcquirer("idtrx");
    sb.send("rtdTrxProducer-out-0",  MessageBuilder.withPayload(t).build());
    return t;
  }

  public void test(Message<Transaction> t) {
    log.info("\n" + t.getPayload().getIdTrxAcquirer() + "\n");
  }
}
