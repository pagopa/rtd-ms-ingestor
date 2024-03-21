package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorDAO;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.WalletContract;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@Import(TestChannelBinderConfiguration.class)
@TestPropertySource(value = {"classpath:application-test.yml"}, inheritProperties = false)
@ExtendWith(OutputCaptureExtension.class)
@ExtendWith(SpringExtension.class)
class BlobRestConnectorTest {

  @Value("${ingestor.resources.base.path}")
  String resources;

  @Value("${ingestor.resources.base.path}/tmp")
  String tmpDirectory;

  @SpyBean
  private BlobRestConnector blobRestConnector;

  @SpyBean
  private EventProcessor blobProcessor;

  @MockBean
  CloseableHttpClient client;
  @MockBean
  IngestorRepository repository;
  @MockBean
  IngestorDAO dao;

  private final String containerRtd = "rtd-transactions-decrypted";

  private final String containerWallet = "wallet-contracts-decrypted";
  private final String blobNameRtd = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp.0.decrypted";

  private final String blobNameWallet = "WALLET.CONTRACTS.20240313.174811.001.json.pgp.0.decrypted";

  private final BlobApplicationAware fakeBlobRtd = new BlobApplicationAware(
      "/blobServices/default/containers/" + containerRtd + "/blobs/" + blobNameRtd);

  private final BlobApplicationAware fakeBlobWallet = new BlobApplicationAware(
      "/blobServices/default/containers/" + containerWallet + "/blobs/" + blobNameWallet);

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
    doThrow(IOException.class).when(client).execute(any(HttpGet.class),
        any(BlobRestConnector.FileDownloadResponseHandler.class));

    BlobApplicationAware blobOut = blobRestConnector.get(fakeBlobRtd);

    verify(client, times(1)).execute(any(HttpUriRequest.class),
        ArgumentMatchers.<ResponseHandler<OutputStream>>any());
    assertEquals(BlobApplicationAware.Status.RECEIVED, blobOut.getStatus());
    assertThat(output.getOut(), containsString("Cannot GET blob "));
  }

  @Test
  void shouldPostContract() throws IOException {

    String serializedContract = "{ \"action\": \"CREATE\", \"import_outcome\": \"OK\", \"payment_method\": \"CARD\", \"method_attributes\": { \"pan_tail\": \"6295\", \"expdate\": \"04/28\", \"card_id_4\": \"6b4d345a594e69654478796546556c384c6955765a42794a345139305457424c394d794e4b4566466c44593d\", \"card_payment_circuit\": \"MC\", \"new_contract_identifier\": \"1e04de1f762b440fa5c444464603bc7c\", \"original_contract_identifier\": \"3b1288edc1f14e0a97129d84fbf1f01e\", \"card_bin\": \"459521\" } }";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonParser jsonParser = new JsonFactory().createJsonParser(serializedContract);
    WalletContract contract = objectMapper.readValue(jsonParser, WalletContract.class);

    CloseableHttpResponse mockedResponse = Mockito.mock(CloseableHttpResponse.class);

    doReturn(mockedResponse).when(client).execute(any(HttpPost.class));
    when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1,
        HttpStatus.SC_OK, contract.getOriginalContractIdentifier()));

    assertTrue(blobRestConnector.postContract(contract));
    verify(client, times(1)).execute(any(HttpPost.class));
  }

  @Test
  void shouldProcess() throws IOException {
    final String transactions = "testTransactions.csv";

    when(repository.findItemByHash(any())).thenReturn(Optional.of(EPIItem.builder()
        .hashPan("b50245d5fee9fa11bead50e7d0afb6c269c77f59474a87442f867ba9643021fc").build()));

    // Create fake file to process
    File decryptedFile = Path.of(tmpDirectory, blobNameRtd).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();

    FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobNameRtd).toString());
    Files.copy(Path.of(resources, transactions), blobDst);

    fakeBlobRtd.setTargetDir(tmpDirectory);
    fakeBlobRtd.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    blobProcessor.process(fakeBlobRtd);
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      assertEquals(blobProcessor.getNumTotalTrx(), blobProcessor.getNumCorrectTrx());
      assertEquals(5, blobProcessor.getNumTotalTrx());
      assertEquals(5, blobProcessor.getNumCorrectTrx());
      assertEquals(Status.PROCESSED, fakeBlobRtd.getStatus());
    });
  }

  @Test
  void shouldProcessWalletEvent() throws IOException {

    // Create fake file to process
    File decryptedFile = Path.of(tmpDirectory, blobNameWallet).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();

    FileOutputStream blobDst = new FileOutputStream(
        Path.of(tmpDirectory, blobNameWallet).toString());
    Files.copy(Path.of(resources, blobNameWallet), blobDst);

    fakeBlobWallet.setTargetDir(tmpDirectory);
    fakeBlobWallet.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    blobProcessor.process(fakeBlobWallet);
    await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
      assertEquals(Status.PROCESSED, fakeBlobWallet.getStatus());
    });
  }

  @Test
  void shouldNotProcessForMissingFile(CapturedOutput output) {

    fakeBlobRtd.setTargetDir(resources);
    fakeBlobRtd.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    fakeBlobRtd.setBlob(blobNameRtd + ".missing");

    blobProcessor.process(fakeBlobRtd);
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      assertThat(output.getOut(), containsString("Extracting transactions from:"));
      assertThat(output.getOut(), containsString("Missing blob file:"));
      assertThat(output.getOut(), not(containsString("Extracted")));
      assertNotEquals(Status.PROCESSED, fakeBlobRtd.getStatus());
    });
  }

  // This test uses a file with all malformed transaction
  // There is one malformed transaction for every field in the object Transaction.
  @Test
  void shouldNotProcessForMalformedFields(CapturedOutput output) throws IOException {
    final String transactions = "testMalformedTransactions.csv";

    when(repository.findItemByHash(any())).thenReturn(Optional.of(EPIItem.builder()
        .hashPan("c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9").build()));

    // Create fake file to process
    File decryptedFile = Path.of(tmpDirectory, blobNameRtd).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();
    FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobNameRtd).toString());
    Files.copy(Path.of(resources, transactions), blobDst);

    fakeBlobRtd.setTargetDir(tmpDirectory);
    fakeBlobRtd.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    blobProcessor.process(fakeBlobRtd);
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      assertEquals(3, blobProcessor.getNumCorrectTrx());
      assertEquals(0, blobProcessor.getNumNotEnrolledCards());
      assertEquals(53, blobProcessor.getNumTotalTrx());

      assertThat(output.getOut(), containsString("Invalid character for Fiscal Code "));
      assertThat(output.getOut(), containsString("Invalid length for Fiscal Code "));
      assertThat(output.getOut(), containsString("Invalid checksum for Fiscal Code "));
      assertEquals(Status.PROCESSED, fakeBlobRtd.getStatus());
    });
  }

  // This test uses a file with all malformed transaction
  // There is one malformed transaction for every field in the object Transaction.
  @ParameterizedTest
  @CsvSource({"testMalformedTransactionHash.csv,",
      "testMalformedTransactionHash_2.csv,3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9",
      "testMalformedTransactionHash_3.csv,ac3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9",
      "testMalformedTransactionHash_4.csv,+3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9"})
  void shouldNotProcessForMalformedEmptyHashPan(String fileName, String hashpan)
      throws IOException {

    final String transactions = fileName;

    when(repository.findItemByHash(any()))
        .thenReturn(Optional.of(EPIItem.builder().hashPan(hashpan).build()));

    // Create fake file to process
    File decryptedFile = Path.of(tmpDirectory, blobNameRtd).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();
    FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobNameRtd).toString());
    Files.copy(Path.of(resources, transactions), blobDst);

    fakeBlobRtd.setTargetDir(tmpDirectory);
    fakeBlobRtd.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    blobProcessor.process(fakeBlobRtd);
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      assertEquals(0, blobProcessor.getNumCorrectTrx());
      assertEquals(1, blobProcessor.getNumTotalTrx());
      assertEquals(0, blobProcessor.getNumNotEnrolledCards());
      assertEquals(Status.PROCESSED, fakeBlobRtd.getStatus());
    });
  }

  @Test
  void shouldNotFailOnEmptyFile() throws IOException {
    final String transactions = "testEmptyFile.csv";

    when(repository.findItemByHash(any())).thenReturn(Optional.of(EPIItem.builder()
        .hashPan("b50245d5fee9fa11bead50e7d0afb6c269c77f59474a87442f867ba9643021fc").build()));

    File decryptedFile = Path.of(tmpDirectory, blobNameRtd).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();
    FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobNameRtd).toString());
    Files.copy(Path.of(resources, transactions), blobDst);

    fakeBlobRtd.setTargetDir(tmpDirectory);
    fakeBlobRtd.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    blobProcessor.process(fakeBlobRtd);

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      assertEquals(0, blobProcessor.getNumCorrectTrx());
    });
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
