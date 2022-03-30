package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import it.gov.pagopa.rtd.ms.rtdmsingestor.event.EventHandlerIntegration;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(topics = {
    "rtd-platform-events"}, partitions = 1,
    bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
@ExtendWith(OutputCaptureExtension.class)
class BlobRestConnectorTest {

  @Autowired
  BlobRestConnector blobRestConnector;

  @MockBean
  CloseableHttpClient client;

  @Autowired
  EventHandlerIntegration eventHandlerIntegration;

  private final String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
  private final String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";

  private BlobApplicationAware blobIn;

  @BeforeEach
  public void setUp() {
    blobIn = new BlobApplicationAware(
        "/blobServices/default/containers/" + container + "/blobs/" + blobName);
  }

  @Test
  void shouldDownload(CapturedOutput output) throws IOException {
    // Improvement idea: mock all the stuff needed in order to allow the FileDownloadResponseHandler
    // class to create a file in a temporary directory and test the content of the downloaded file
    // for an expected content.
    OutputStream mockedOutputStream = mock(OutputStream.class);
    doReturn(mockedOutputStream).when(client)
        .execute(any(HttpGet.class), any(BlobRestConnector.FileDownloadResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnector.download(blobIn);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        ArgumentMatchers.<ResponseHandler<OutputStream>>any());
    assertEquals(BlobApplicationAware.Status.DOWNLOADED, blobOut.getStatus());
    assertThat(output.getOut(), not(containsString("GET Blob failed")));
  }


  @Test
  void shouldFailDownload(CapturedOutput output) throws IOException {
    doThrow(IOException.class).when(client)
        .execute(any(HttpGet.class), any(BlobRestConnector.FileDownloadResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnector.download(blobIn);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        ArgumentMatchers.<ResponseHandler<OutputStream>>any());
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("GET Blob failed"));
  }

  @Test
  void shouldProduce(CapturedOutput output) {
    blobRestConnector.produce(blobIn);
  }

}
