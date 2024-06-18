package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorDAO;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.ContractMethodAttributes;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
class EventProcessorTest {

  @Value("${ingestor.resources.base.path}")
  String resources;

  @Value("${ingestor.resources.base.path}/tmp")
  String tmpDirectory;

  private final String containerWallet = "wallet-contracts-decrypted";

  private final String blobNameWallet = "WALLET.CONTRACTS.20240313.174811.001.json.pgp.0.decrypted";

  private final String blobNameWalletMalformed = "WALLET.CONTRACTS.20240402.103010.001.json.pgp.0.decrypted";

  private final BlobApplicationAware fakeBlobWallet = new BlobApplicationAware(
      "/blobServices/default/containers/" + containerWallet + "/blobs/" + blobNameWallet);

  private final BlobApplicationAware fakeBlobWalletMalformed = new BlobApplicationAware(
      "/blobServices/default/containers/" + containerWallet + "/blobs/" + blobNameWalletMalformed);

  @MockBean
  CloseableHttpClient client;
  @MockBean
  IngestorRepository repository;
  @MockBean
  IngestorDAO dao;
  @SpyBean
  private EventProcessor blobProcessor;

  @MockBean
  private BlobRestConnector connector;

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

    doReturn(true).when(connector).postContract(any(ContractMethodAttributes.class), any(String.class));
    doReturn(true).when(connector).deleteContract(any(String.class), any(String.class));


    blobProcessor.process(fakeBlobWallet);
    await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
      assertEquals(Status.PROCESSED, fakeBlobWallet.getStatus());
    });
  }

  @Test
  void shouldNotProcessWalletEventFailedRequest() throws IOException {

    // Create fake file to process
    File decryptedFile = Path.of(tmpDirectory, blobNameWalletMalformed).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();

    FileOutputStream blobDst = new FileOutputStream(
        Path.of(tmpDirectory, blobNameWallet).toString());
    Files.copy(Path.of(resources, blobNameWallet), blobDst);

    fakeBlobWallet.setTargetDir(tmpDirectory);
    fakeBlobWallet.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    doReturn(false).when(connector).postContract(any(ContractMethodAttributes.class), any(String.class));

    blobProcessor.process(fakeBlobWallet);
    await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
      assertEquals(Status.PROCESSED, fakeBlobWallet.getStatus());
    });
  }

  @Test
  void shouldNotProcessWalletEventMalformedContracts() throws IOException {

    // Create fake file to process
    File decryptedFile = Path.of(tmpDirectory, blobNameWalletMalformed).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();

    FileOutputStream blobDst = new FileOutputStream(
        Path.of(tmpDirectory, blobNameWalletMalformed).toString());
    Files.copy(Path.of(resources, blobNameWalletMalformed), blobDst);

    fakeBlobWalletMalformed.setTargetDir(tmpDirectory);
    fakeBlobWalletMalformed.setStatus(BlobApplicationAware.Status.DOWNLOADED);

    doReturn(false).when(connector).postContract(any(ContractMethodAttributes.class), any(String.class));

    blobProcessor.process(fakeBlobWalletMalformed);
    await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
      assertEquals(Status.PROCESSED, fakeBlobWalletMalformed.getStatus());
    });
  }
}
