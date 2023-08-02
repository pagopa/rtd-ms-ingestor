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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.validation.annotation.Validated;

/**
 * This class contains handling methods for blobs.
 */
@Service
@Slf4j
@Validated
@RequiredArgsConstructor
public class BlobRestConnector implements TransactionCheck {

  @Value("${ingestor.api.baseurl}")
  private String baseUrl;

  @Value("${ingestor.blobclient.apikey}")
  private String blobApiKey;

  @Value("${ingestor.blobclient.basepath}")
  private String blobBasePath;

  private final CloseableHttpClient httpClient;

  private final StreamBridge sb;

  private final IngestorRepository repository;

  private int numNotEnrolledCards = 0;
  private int numCorrectTrx = 0;
  private int numTotalTrx = 0;

  /**
   * Method that allows the get of the blob from a remote storage.
   *
   * @param blob a blob that has been received from the event hub but not downloaded.
   * @return a locally available blob
   */
  public BlobApplicationAware get(BlobApplicationAware blob) {
    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getContainer() + "/" + blob.getBlob();
    final HttpGet getBlob = new HttpGet(uri);
    getBlob.setHeader(new BasicHeader("Ocp-Apim-Subscription-Key", blobApiKey));

    try {
      OutputStream result = httpClient.execute(getBlob, new FileDownloadResponseHandler(
          new FileOutputStream(Path.of(blob.getTargetDir(), blob.getBlob()).toFile())));
      result.close();
      blob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
      log.info("Successful GET of blob {} from {}", blob.getBlob(), blob.getContainer());
    } catch (Exception ex) {
      log.error("Cannot GET blob {} from {}: {}", blob.getBlob(), blob.getContainer(),
          ex.getMessage());
    }

    return blob;
  }

  /**
   * Method that maps transaction fields taken them from csv into Transaction object, then send it
   * on the output queue. This is done for each transaction inside the blob received.
   *
   * @param blob the blob of the transaction.
   */
  public BlobApplicationAware process(BlobApplicationAware blob) {
    log.info("Extracting transactions from:{}", blob.getBlobUri());

    FileReader fileReader;

    numTotalTrx = 0;
    numCorrectTrx = 0;
    numNotEnrolledCards = 0;

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

  /**
   * Method that allows the deletion of the blob from a remote storage.
   *
   * @param blob a blob that has been processed and have to be removed both remotely ad locally.
   * @return a remotely deleted blob with REMOTELY_DELETED status.
   */
  public BlobApplicationAware deleteRemote(BlobApplicationAware blob) {
    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getContainer() + "/" + blob.getBlob();
    final HttpDelete deleteBlob = new HttpDelete(uri);
    deleteBlob.setHeader(new BasicHeader("Ocp-Apim-Subscription-Key", blobApiKey));
    deleteBlob.setHeader(new BasicHeader("x-ms-version", "2021-04-10"));

    try (CloseableHttpResponse myResponse = httpClient.execute(deleteBlob)) {
      int statusCode = myResponse.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_ACCEPTED) {
        blob.setStatus(Status.REMOTELY_DELETED);
        log.info("Remote blob {} deleted successfully", uri);
      } else {
        log.error("Can't delete blob {}. Invalid HTTP response: {}, {}", uri, statusCode,
            myResponse.getStatusLine().getReasonPhrase());
      }
    } catch (Exception ex) {
      log.error("Can't delete blob {}. Unexpected error: {}", uri, ex.getMessage());
    }

    return blob;
  }

  static class FileDownloadResponseHandler implements ResponseHandler<OutputStream> {

    private final OutputStream target;

    public FileDownloadResponseHandler(OutputStream target) {
      this.target = target;
    }

    @Override
    public OutputStream handleResponse(HttpResponse response) throws IOException {
      StreamUtils.copy(Objects.requireNonNull(response.getEntity().getContent()), this.target);
      return this.target;
    }
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

  @Override
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
        // DeadLetterQueueEvent dlqException = new DeadLetterQueueEvent(t, ex.getMessage());
        // sb.send("rtdDlqTrxProducer-out-0", MessageBuilder.withPayload(dlqException).build());
        log.error("Error getting records : {}", ex.getMessage());
      }
    });
  }
}
