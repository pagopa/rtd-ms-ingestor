package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import it.gov.pagopa.rtd.ms.rtdmsingestor.adapter.ContractAdapter;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Application;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.WalletContract;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import it.gov.pagopa.rtd.ms.rtdmsingestor.utils.Anonymizer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
@RequiredArgsConstructor
public class EventProcessor {

  private static final String CREATE_ACTION = "CREATE";
  private static final String DELETE_ACTION = "DELETE";

  private final StreamBridge sb;

  private final IngestorRepository repository;
  private int numNotEnrolledCards;
  private int numCorrectTrx;
  private int numTotalTrx;

  private int numTotalContracts;
  private int numFailedContracts;

  private final BlobRestConnector connector;

  private final ContractAdapter adapter;

  private final Anonymizer anonymizer;

  public BlobApplicationAware process(BlobApplicationAware blob) {
    Path blobPath = Path.of(blob.getTargetDir(), blob.getBlob());
    if (Application.RTD.equals(blob.getApp())) {
      return processRtdTransaction(blob, blobPath);
    } else if (Application.WALLET.equals(blob.getApp())) {
      return processWalletContracts(blob, blobPath);
    }
    return blob;
  }

  /**
   * Method that maps transaction fields taken them from csv into Transaction object, then send it
   * on the output queue. This is done for each transaction inside the blob received.
   *
   * @param blob the blob of the transaction.
   */
  private BlobApplicationAware processRtdTransaction(BlobApplicationAware blob, Path blobPath) {
    log.info("Extracting transactions from:{}", blob.getBlobUri());

    numNotEnrolledCards = 0;
    numCorrectTrx = 0;
    numTotalTrx = 0;

    FileReader fileReader;

    try {
      fileReader = new FileReader(blobPath.toFile());
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

  private BlobApplicationAware processWalletContracts(BlobApplicationAware blob, Path blobPath) {
    log.info("Extracting contracts from:{}", blob.getBlobUri());

    numFailedContracts = 0;
    numTotalContracts = 0;

    ObjectMapper objectMapper = new ObjectMapper();
    JsonFactory jsonFactory = new JsonFactory();

    try (InputStream inputStream = new FileInputStream(blobPath.toFile())) {
      JsonParser jsonParser = jsonFactory.createParser(inputStream);

      if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
        log.error("Validation error: expected wallet export contracts array");
        return blob;
      }

      while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
        numTotalContracts++;

        WalletContract contract = objectMapper.readValue(jsonParser, WalletContract.class);
        processContractWrapper(blob.getBlob(), contract);
      }
    } catch (JsonParseException | MismatchedInputException e) {
      log.error("Validation error: malformed wallet export");
      return blob;
    } catch (IOException e) {
      log.error("Missing blob file:{}", blobPath);
      return blob;
    }

    if (numFailedContracts == 0) {
      log.info("Blob {} processed successfully", blob.getBlob());
      blob.setStatus(Status.PROCESSED);
    }

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

  @WithSpan
  private void processContractWrapper(String fileName, WalletContract contract)
      throws JsonProcessingException {
    boolean updateOutcome = processContract(contract);
    if (!updateOutcome) {
      numFailedContracts++;
    }
    MDC.put("Filename", fileName);
    MDC.put("Position", String.valueOf(numTotalContracts));
    MDC.put("Action", contract.getAction());
    MDC.put("ImportOutcome", contract.getImportOutcome());
    MDC.put("Successful", String.valueOf(updateOutcome));
    log.info("");
    MDC.clear();
  }

  private boolean processContract(WalletContract contract)
      throws JsonProcessingException {

    if (contract.getAction() == null) {
      log.error("Null action on contract at {}", numTotalContracts);
      return false;
    }

    contract = adapter.adapt(contract);
    String currContractId;
    String contractIdHmac;

    if (!"OK".equals(contract.getImportOutcome())) {
      currContractId = contract.getContractIdentifier();
      contractIdHmac = anonymizer.anonymize(currContractId);
      MDC.put("ImportOutcome", contract.getImportOutcome());
      MDC.put("ContractID", contractIdHmac);
      return true;
    }

    if (contract.getAction().equals(CREATE_ACTION)) {
      currContractId = contract.getMethodAttributes().getContractIdentifier();
      contractIdHmac = anonymizer.anonymize(currContractId);
      MDC.put("ContractID", contractIdHmac);
      if (!connector.postContract(contract.getMethodAttributes(), contractIdHmac)) {
        log.error("Failed saving contract at position {}", numTotalContracts);
        return false;
      } else {
        return true;
      }
    } else if (contract.getAction().equals(DELETE_ACTION)) {
      currContractId = contract.getContractIdentifier();
      contractIdHmac = anonymizer.anonymize(currContractId);
      MDC.put("ContractID", contractIdHmac);
      if (!connector.deleteContract(contract.getContractIdentifier(), contractIdHmac)) {
        log.error("Failed deleting contract at position {}", numTotalContracts);
        return false;
      } else {
        return true;
      }
    }
    log.error("Unrecognized action on contract at position {}", numTotalContracts);
    return false;
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
