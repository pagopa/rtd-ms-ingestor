package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import it.gov.pagopa.rtd.ms.rtdmsingestor.event.EventHandler;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.PaymentInstrumentItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.BlobRestConnector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EmbeddedKafka(topics = {"rtd-platform-events"}, partitions = 1,
    bootstrapServersProperty = "spring.embedded.kafka.brokers")
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class})
@TestPropertySource(value = {"classpath:application-test.yml"}, inheritProperties = false)
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
@ContextConfiguration(classes = {EventHandler.class})
class IngestorRepositoryTest {

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

  @MockBean
  IngestorRepository repository;

  private final String container = "rtd-transactions-decrypted";
  private final String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp.0.decrypted";

  private BlobApplicationAware fakeBlob = new BlobApplicationAware(
      "/blobServices/default/containers/" + container + "/blobs/" + blobName);


  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }

  @Test
  void testHashReplacement(CapturedOutput output) throws IOException{

    String transactions = "testHashReplacement.csv";
    when(repository.findItemByHash(any()))
      .thenReturn(Optional.of(new PaymentInstrumentItem("c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9")));

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
      assertEquals(0,blobRestConnector.getNumNotEnrolledCards());
      assertEquals(1,blobRestConnector.getNumTotalTrx());
      assertEquals(1,blobRestConnector.getNumCorrectTrx());
      assertEquals(Status.PROCESSED, fakeBlob.getStatus());
    });
  }

  @Test
  void testHashReplacementNotEnrolledCard() throws IOException{

    String transactions = "testHashReplacement.csv";
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
      assertEquals(1,blobRestConnector.getNumNotEnrolledCards());
      assertEquals(1,blobRestConnector.getNumTotalTrx());
      assertEquals(0,blobRestConnector.getNumCorrectTrx());
      assertEquals(Status.PROCESSED, fakeBlob.getStatus());
    });

  }

}
