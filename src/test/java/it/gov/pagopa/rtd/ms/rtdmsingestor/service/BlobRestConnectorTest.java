package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class BlobRestConnectorTest {

  private final String tmpDirectory = "src/test/resources/tmp";

  private BlobRestConnector blobRestConnector;
  private CloseableHttpClient client;

  private final String containerRtd = "rtd-transactions-decrypted";
  private final String blobNameRtd = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp.0.decrypted";
  private final BlobApplicationAware fakeBlobRtd = new BlobApplicationAware(
      "/blobServices/default/containers/" + containerRtd + "/blobs/" + blobNameRtd);

  @BeforeEach
  void setup() {
    client = mock(CloseableHttpClient.class);
    blobRestConnector = new BlobRestConnector(client);
  }

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }

  @Test
  void shouldDownload(CapturedOutput output) throws IOException {
    // Improvement idea: mock all the stuff needed in order to allow the
    // FileDownloadResponseHandler
    // class to create a file in a temporary directory and test the content of the
    // downloaded file
    // for an expected content.

    // Create the mocked output stream to simulate the blob get
    File decryptedFile = Path.of(tmpDirectory, blobNameRtd).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();
    fakeBlobRtd.setTargetDir(tmpDirectory);
    OutputStream mockedOutputStream = mock(OutputStream.class);

    doReturn(mockedOutputStream).when(client).execute(any(HttpGet.class),
        any(BlobRestConnector.FileDownloadResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnector.get(fakeBlobRtd);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        ArgumentMatchers.<ResponseHandler<OutputStream>>any());
    assertEquals(BlobApplicationAware.Status.DOWNLOADED, blobOut.getStatus());
    assertThat(output.getOut(), not(containsString("Cannot GET blob ")));
  }

  @Test
  void shouldFailDownload(CapturedOutput output) throws IOException {
    doThrow(IOException.class).when(client).execute(any(HttpUriRequest.class), any(ResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnector.get(fakeBlobRtd);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        ArgumentMatchers.<ResponseHandler<OutputStream>>any());
    assertEquals(Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot GET blob "));
  }

  @Test
  void shouldDelete(CapturedOutput output) throws IOException {

    CloseableHttpResponse mockedResponse = mock(CloseableHttpResponse.class);
    when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1,
        HttpStatus.SC_ACCEPTED, "Blob successfully deleted."));

    doReturn(mockedResponse).when(client).execute(any(HttpDelete.class));

    blobRestConnector.deleteRemote(fakeBlobRtd);

    verify(client, times(1)).execute(any(HttpUriRequest.class));
    assertEquals(Status.REMOTELY_DELETED, fakeBlobRtd.getStatus());
    assertThat(output.getOut(), not(containsString("Can't delete blob")));
    assertThat(output.getOut(), containsString(fakeBlobRtd.getBlob() + " deleted successfully"));
  }

  @Test
  void shouldFailDeleteExceptionOnHttpCall(CapturedOutput output) throws IOException {

    doThrow(new IOException("Connection problem.")).when(client).execute(any(HttpDelete.class));

    blobRestConnector.deleteRemote(fakeBlobRtd);

    verify(client, times(1)).execute(any(HttpUriRequest.class));
    assertNotEquals(Status.REMOTELY_DELETED, fakeBlobRtd.getStatus());
    assertThat(output.getOut(), containsString("Unexpected error:"));
  }

  @Test
  void shouldFailDeleteWrongStatusCode(CapturedOutput output) throws IOException {

    CloseableHttpResponse mockedResponse = mock(CloseableHttpResponse.class);
    when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1,
        HttpStatus.SC_FORBIDDEN, "Authentication failed."));

    doReturn(mockedResponse).when(client).execute(any(HttpDelete.class));

    blobRestConnector.deleteRemote(fakeBlobRtd);

    verify(client, times(1)).execute(any(HttpUriRequest.class));
    assertNotEquals(Status.REMOTELY_DELETED, fakeBlobRtd.getStatus());
    assertThat(output.getOut(), containsString("Invalid HTTP response:"));
  }

}
