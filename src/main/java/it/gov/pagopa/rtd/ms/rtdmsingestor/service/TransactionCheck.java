package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import java.util.stream.Stream;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;

public interface TransactionCheck {
   public void transactionCheckProcess(Stream<Transaction> readTransaction);
}
