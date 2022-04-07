package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EmbeddedKafka(topics = {"rtd-platform-events"}, partitions = 1,
    bootstrapServersProperty = "spring.embedded.kafka.brokers")
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class})
@TestPropertySource(value = {"classpath:application-test.yml"}, inheritProperties = false)
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class BlobRestConnectorTest {

  @Value("${ingestor.resources.base.path}")
  String resources;

  @Value("${ingestor.resources.base.path}/tmp")
  String tmpDirectory;

  @Autowired
  private StreamBridge stream;

  @SpyBean
  private BlobApplicationAware blobApplicationAware;

  @SpyBean
  private BlobRestConnector blobRestConnector;

  @MockBean
  CloseableHttpClient client;

  private final String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
  private final String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp.decrypted";

  private BlobApplicationAware fakeBlob = new BlobApplicationAware(
      "/blobServices/default/containers/" + container + "/blobs/" + blobName);

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }


  @Test
  void shouldDownload(CapturedOutput output) throws IOException {
    // Improvement idea: mock all the stuff needed in order to allow the FileDownloadResponseHandler
    // class to create a file in a temporary directory and test the content of the downloaded file
    // for an expected content.

    //Create the mocked output stream to simulate the blob download
    File decryptedFile = Path.of(tmpDirectory, blobName).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();
    fakeBlob.setTargetDir(tmpDirectory);
    OutputStream mockedOutputStream = mock(OutputStream.class);

    doReturn(mockedOutputStream).when(client)
        .execute(any(HttpGet.class), any(BlobRestConnector.FileDownloadResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnector.download(fakeBlob);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        ArgumentMatchers.<ResponseHandler<OutputStream>>any());
    assertEquals(BlobApplicationAware.Status.DOWNLOADED, blobOut.getStatus());
    assertThat(output.getOut(), not(containsString("GET Blob failed")));
  }


  @Test
  void shouldFailDownload(CapturedOutput output) throws IOException {
    doThrow(IOException.class).when(client)
        .execute(any(HttpGet.class), any(BlobRestConnector.FileDownloadResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnector.download(fakeBlob);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        ArgumentMatchers.<ResponseHandler<OutputStream>>any());
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("GET Blob failed"));
  }


  @Test
  void shouldProcess(CapturedOutput output) throws IOException {
    String transactions = "testTransactions.csv";

    //Create fake file to process
    File decryptedFile = Path.of(tmpDirectory, blobName).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();
    FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobName).toString());
    Files.copy(Path.of(resources, transactions), blobDst);

    fakeBlob.setTargetDir(tmpDirectory);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    blobRestConnector.process(fakeBlob);
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      assertThat(output.getOut(), containsString("Extracting transactions from:"));
      assertThat(output.getOut(), containsString("Extracted 5 transactions from:"));
      assertThat(output.getOut(), containsString("Received transaction:"));
      assertEquals(Status.PROCESSED, fakeBlob.getStatus());
    });
  }

  @Test
  void shouldNotProcessForMissingFile(CapturedOutput output) {

    fakeBlob.setTargetDir(resources);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    fakeBlob.setBlob(blobName + ".missing");

    blobRestConnector.process(fakeBlob);
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      assertThat(output.getOut(), containsString("Extracting transactions from:"));
      assertThat(output.getOut(), containsString("Missing blob file:"));
      assertThat(output.getOut(), not(containsString("Extracted 5 transactions from:")));
      assertThat(output.getOut(), not(containsString("Received transaction:")));
      assertNotEquals(Status.PROCESSED, fakeBlob.getStatus());
    });
  }


}
