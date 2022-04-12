package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import com.opencsv.bean.CsvToBeanBuilder;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

/**
 * This class contains handling methods for blobs.
 */
@Service
@Slf4j
public class BlobRestConnector {

  @Value("${ingestor.api.baseurl}")
  private String baseUrl;

  @Value("${ingestor.blobclient.apikey}")
  private String blobApiKey;

  @Value("${ingestor.blobclient.basepath}")
  private String blobBasePath;

  @Autowired
  CloseableHttpClient httpClient;

  @Autowired
  StreamBridge sb;

  /**
   * Method that allows the download of the blob from a remote storage.
   *
   * @param blob a blob that has been received from the event hub but not downloaded.
   * @return a locally available blob
   */
  public BlobApplicationAware download(BlobApplicationAware blob) {
    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getContainer() + "/" + blob.getBlob();
    final HttpGet getBlob = new HttpGet(uri);
    getBlob.setHeader(new BasicHeader("Ocp-Apim-Subscription-Key", blobApiKey));

    try {
      OutputStream result = httpClient.execute(getBlob,
          new FileDownloadResponseHandler(
              new FileOutputStream(Path.of(blob.getTargetDir(), blob.getBlob()).toFile())));
      result.close();
      blob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    } catch (Exception ex) {
      log.error("{}, GET Blob failed:{}", ex, blob.getBlobUri());
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

    boolean failProduce = false;

    int numTrx = 0;

    String blobPath = Path.of(blob.getTargetDir(), blob.getBlob()).toString();

    try (
        LineIterator it = FileUtils.lineIterator(
            Path.of(blobPath).toFile(), "UTF-8")
    ) {
      while (it.hasNext()) {
        //Get a StringReader from the next line of the blob
        StringReader line = new StringReader(it.nextLine());
        //Obtain the (only) Transaction object parsed from the csv line
        //Read in batch is possible but requires a change in the use of line iterator
        Transaction t = new CsvToBeanBuilder<Transaction>(line).withSeparator(';')
            .withType(Transaction.class)
            .build().parse().get(0);
        sb.send("rtdTrxProducer-out-0", MessageBuilder.withPayload(t).build());
        log.info(t.toString());
        numTrx++;
      }
    } catch (IOException e) {
      failProduce = true;
      log.error("Missing blob file:{}", blobPath);
    }

    if (!failProduce) {
      log.info("Extracted {} transactions from:{}", numTrx, blob.getBlobUri());
      blob.setStatus(Status.PROCESSED);
    }

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
}
