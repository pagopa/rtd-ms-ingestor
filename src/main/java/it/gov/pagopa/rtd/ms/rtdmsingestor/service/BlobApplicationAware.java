package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

/**
 * Thi calss represents the mapping of a blob storage to Java object.
 */
@Service
@Slf4j
public class BlobApplicationAware {

  @Autowired
  StreamBridge sb;

  String uri;

  public BlobApplicationAware init(String uri) {
    this.uri = uri;
    return this;
  }

  public BlobApplicationAware download(BlobApplicationAware blob) {
    return this;
  }

  public BlobApplicationAware open(BlobApplicationAware blob) {
    return this;
  }

  /**
   * TEMPORARY IMPL - Method that currently set the Acquirer Id to "idtrx".
   *
   * @param blob the blob of the transaction.
   * @return the transaction with the Acquiredr id set to "idtrx".
   */
  public Transaction produce(BlobApplicationAware blob) {
    Transaction t = new Transaction();
    t.setIdTrxAcquirer("idtrx");
    return t;
  }

  public void test(Message<Transaction> t) {
    log.info("\n" + t.getPayload().getIdTrxAcquirer() + "\n");
  }
}
