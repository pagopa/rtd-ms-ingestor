package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
@RequiredArgsConstructor
public class EventProcessor {

  private final StreamBridge sb;

  private final IngestorRepository repository;
  private int numNotEnrolledCards;
  private int numCorrectTrx;
  private int numTotalTrx;

  public BlobApplicationAware process(BlobApplicationAware blob) {
    return processRtdTransaction(blob);
  }

  /**
   * Method that maps transaction fields taken them from csv into Transaction object, then send it
   * on the output queue. This is done for each transaction inside the blob received.
   *
   * @param blob the blob of the transaction.
   */
  private BlobApplicationAware processRtdTransaction(BlobApplicationAware blob) {
    log.info("Extracting transactions from:{}", blob.getBlobUri());

    numNotEnrolledCards = 0;
    numCorrectTrx = 0;
    numTotalTrx = 0;

    FileReader fileReader;

    try {
      fileReader = new FileReader(Path.of(blob.getTargetDir(), blob.getBlob()).toFile());
    } catch (FileNotFoundException e) {
      log.error("Missing blob file: {}", blob.getBlob());
      return blob;
    }

    BeanVerifier<Transaction> verifier = new TransactionVerifier();

    CsvToBeanBuilder<Transaction> builder =
        new CsvToBeanBuilder<Transaction>(fileReader).withType(Transaction.class).withSeparator(';')
            .withVerifier(verifier).withThrowExceptions(false);

    CsvToBean<Transaction> csvToBean = builder.build();
    Stream<Transaction> readTransaction = csvToBean.stream();

    transactionCheckProcess(readTransaction);

    List<CsvException> violations = csvToBean.getCapturedExceptions();

    numTotalTrx = numTotalTrx + violations.size();

    if (!violations.isEmpty()) {
      for (CsvException e : violations) {
        log.error("Validation error at line " + e.getLineNumber() + " : " + e.getMessage());
      }
    } else if (numTotalTrx == 0) {
      log.error("No records found in file {}", blob.getBlob());
    }

    if (numTotalTrx == numCorrectTrx) {
      log.info("Extraction result: extracted all {} transactions from:{}", numCorrectTrx,
          blob.getBlobUri());
    } else {
      log.info(
          "Extraction result: {} well formed transactions and {} "
              + "not enrolled cards out of {} rows extracted from:{}",
          numCorrectTrx, numNotEnrolledCards, numTotalTrx, blob.getBlobUri());
    }

    blob.setStatus(Status.PROCESSED);
    return blob;
  }

  public void transactionCheckProcess(Stream<Transaction> readTransaction) {
    readTransaction.forEach(t -> {
      try {
        Optional<EPIItem> dbResponse = repository.findItemByHash(t.getHpan());
        if (dbResponse.isPresent()) {
          t.setHpan(dbResponse.get().getHashPan());
          sb.send("rtdTrxProducer-out-0", MessageBuilder.withPayload(t).build());
          numCorrectTrx++;
        } else {
          numNotEnrolledCards++;
        }
        numTotalTrx++;
      } catch (Exception ex) {
        log.error("Error getting records : {}", ex.getMessage());
      }
    });
  }

  protected int getNumNotEnrolledCards() {
    return numNotEnrolledCards;
  }

  protected int getNumTotalTrx() {
    return numTotalTrx;
  }

  protected int getNumCorrectTrx() {
    return numCorrectTrx;
  }
}
