package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import lombok.extern.slf4j.Slf4j;

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

  public Transaction produce(BlobApplicationAware blob) {
    Transaction t = new Transaction();
    t.setIdTrxAcquirer("idtrx");
    return t;
  }

  public void test(Message<Transaction> t) {
    log.info("\n"+t.getPayload().getIdTrxAcquirer()+"\n");
  }
}
