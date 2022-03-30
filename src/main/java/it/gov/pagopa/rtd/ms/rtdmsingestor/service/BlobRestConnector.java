package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
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
   * TEMPORARY IMPL - Method that currently set the Acquirer Id to "idtrx".
   *
   * @param blob the blob of the transaction.
   * @return the transaction with the Acquiredr id set to "idtrx".
   */
  public Transaction produce(BlobApplicationAware blob) {
    log.info("Produce: {}", blob.getBlobUri());
    Transaction t = new Transaction();
    t.setIdTrxAcquirer("idtrx");
    sb.send("rtdTrxProducer-out-0", MessageBuilder.withPayload(t).build());
    return t;
  }

  public void test(Message<Transaction> t) {
    log.info("\n" + t.getPayload().getIdTrxAcquirer() + "\n");
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
