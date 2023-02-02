package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import java.util.stream.Stream;

public interface TransactionCheck {
  public void transactionCheckProcess(Stream<Transaction> readTransaction);
}
