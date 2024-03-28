package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.ContractMethodAttributes;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
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
public class BlobRestConnector {

  @Value("${ingestor.api.baseurl}")
  private String baseUrl;

  @Value("${ingestor.api.wallet.baseurl}")
  private String walletBaseUrl;

  @Value("${ingestor.api.wallet.updateContracts}")
  private String updateContractsEndpoint;

  @Value("${ingestor.api.wallet.deleteContracts}")
  private String deleteContractsEndpoint;

  @Value("${ingestor.blobclient.apikey}")
  private String blobApiKey;

  @Value("${ingestor.api.wallet.apikey}")
  private String walletApiKey;

  @Value("${ingestor.blobclient.basepath}")
  private String blobBasePath;

  private final CloseableHttpClient httpClient;

  private static final String APIM_SUBSCRIPTION_HEADER = "Ocp-Apim-Subscription-Key";


  /**
   * Method that allows the get of the blob from a remote storage.
   *
   * @param blob a blob that has been received from the event hub but not downloaded.
   * @return a locally available blob
   */
  public BlobApplicationAware get(BlobApplicationAware blob) {
    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getContainer() + "/" + blob.getBlob();
    final HttpGet getBlob = new HttpGet(uri);
    getBlob.setHeader(new BasicHeader(APIM_SUBSCRIPTION_HEADER, blobApiKey));

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
   * Method that allows the deletion of the blob from a remote storage.
   *
   * @param blob a blob that has been processed and have to be removed both remotely ad locally.
   * @return a remotely deleted blob with REMOTELY_DELETED status.
   */
  public BlobApplicationAware deleteRemote(BlobApplicationAware blob) {
    String uri = baseUrl + "/" + blobBasePath + "/" + blob.getContainer() + "/" + blob.getBlob();
    final HttpDelete deleteBlob = new HttpDelete(uri);
    deleteBlob.setHeader(new BasicHeader(APIM_SUBSCRIPTION_HEADER, blobApiKey));
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

  public boolean postContract(ContractMethodAttributes contract) throws JsonProcessingException {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String contractJson = ow.writeValueAsString(contract);
    StringEntity contractEntity = new StringEntity(
        contractJson,
        ContentType.APPLICATION_JSON);

    String uri = walletBaseUrl + updateContractsEndpoint;
    final HttpPost postContract = new HttpPost(uri);
    postContract.setEntity(contractEntity);
    postContract.setHeader(new BasicHeader(APIM_SUBSCRIPTION_HEADER, walletApiKey));

    try (CloseableHttpResponse myResponse = httpClient.execute(postContract)) {
      int statusCode = myResponse.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        log.info("Successfully updated contract {}", contract);
        return true;
      } else {
        log.error("Can't update contract {}. Invalid HTTP response: {}, {}", contract, statusCode,
            myResponse.getStatusLine().getReasonPhrase());
        return false;
      }
    } catch (Exception ex) {
      log.error("Can't update contract {}. Unexpected error: {}", contract, ex.getMessage());
      return false;
    }
  }

  public boolean deleteContract(String contractIdentifier) throws JsonProcessingException {
    String uri = walletBaseUrl + deleteContractsEndpoint;
    final HttpPost deleteContract = new HttpPost(uri);
    deleteContract.setHeader(new BasicHeader(APIM_SUBSCRIPTION_HEADER, walletApiKey));
    StringEntity newContractIdentifierEntity = new StringEntity(
        "{\"contractIdentifier\": \"" + contractIdentifier + "\"}",
        ContentType.APPLICATION_JSON);
    deleteContract.setEntity(newContractIdentifierEntity);

    try (CloseableHttpResponse myResponse = httpClient.execute(deleteContract)) {
      int statusCode = myResponse.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        log.info("Successfully delete contract {}", contractIdentifier);
        return true;
      } else {
        log.error("Can't delete contract {}. Invalid HTTP response: {}, {}", contractIdentifier,
            statusCode,
            myResponse.getStatusLine().getReasonPhrase());
        return false;
      }
    } catch (Exception ex) {
      log.error("Can't delete contract {}. Unexpected error: {}", contractIdentifier,
          ex.getMessage());
      return false;
    }
  }

}
