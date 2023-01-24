package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import it.gov.pagopa.rtd.ms.rtdmsingestor.event.EventHandler;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.mongodb.MongoException;
import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EmbeddedKafka(topics = {"rtd-platform-events","rtd-dlq-trx"}, partitions = 1,
    bootstrapServersProperty = "spring.embedded.kafka.brokers")
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class})
@TestPropertySource(value = {"classpath:application-test.yml"}, inheritProperties = false)
@DirtiesContext
@ContextConfiguration(classes = {EventHandler.class})
class DeadLetterQueueProcessorTest {

  @Value("${ingestor.resources.base.path}")
  String resources;

  @Value("${ingestor.resources.base.path}/tmp")
  String tmpDirectory;

  @SpyBean
  private BlobApplicationAware blobApplicationAware;

  @SpyBean
  private BlobRestConnector blobRestConnector;

  @SpyBean
  private DeadLetterQueueProcessor deadLetterQueueProcessor;

  @MockBean
  CloseableHttpClient client;

  @MockBean
  IngestorRepository repository;


  private final String container = "rtd-transactions-decrypted";
  private final String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp.0.decrypted";

  private BlobApplicationAware fakeBlob = new BlobApplicationAware(
      "/blobServices/default/containers/" + container + "/blobs/" + blobName);

  //This counter represents the number of fiscal codes that are malformed in the test file.
  // The corresponding transactions are not discarded, instead an error is logged and the
  // transaction is processed anyway.
  int malformedBuyProcessedFiscalCodes = 3;

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }

  @Test
  void mongoQueryError() throws IOException {
    String transactions = "testHashReplacement.csv";

    when(repository.findItemByHash(any()))
      .thenThrow(new MongoException("Exception 429"));

    //Create fake file to process
    File decryptedFile = Path.of(tmpDirectory, blobName).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();

    FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobName).toString());
    Files.copy(Path.of(resources, transactions), blobDst);

    fakeBlob.setTargetDir(tmpDirectory);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    FileReader fileReader = new FileReader(Path.of(fakeBlob.getTargetDir(), fakeBlob.getBlob()).toFile());

    BeanVerifier<Transaction> verifier = new TransactionVerifier();

    CsvToBeanBuilder<Transaction> builder = new CsvToBeanBuilder<Transaction>(fileReader)
        .withType(Transaction.class)
        .withSeparator(';')
        .withVerifier(verifier)
        .withThrowExceptions(false);

    CsvToBean<Transaction> csvToBean = builder.build();
    Stream<Transaction> readTransaction = csvToBean.stream();

    readTransaction.map(e -> {
        return Stream.of(e);
    }).forEach(e -> {
        deadLetterQueueProcessor.TransactionCheckProcess(e);
    });

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
        assertEquals(0, deadLetterQueueProcessor.getProcessedTrx());
        assertEquals(1,deadLetterQueueProcessor.getExcepitonTrx());
    });
  }


  @Test
  void deadLetterQueueCorrectProcessing() throws IOException {
    String transactions = "testHashReplacement.csv";

    when(repository.findItemByHash(any()))
        .thenReturn(Optional.of( EPIItem
        .builder()
        .hashPan("b50245d5fee9fa11bead50e7d0afb6c269c77f59474a87442f867ba9643021fc")
        .build()));

    //Create fake file to process
    File decryptedFile = Path.of(tmpDirectory, blobName).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();

    FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobName).toString());
    Files.copy(Path.of(resources, transactions), blobDst);

    fakeBlob.setTargetDir(tmpDirectory);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    FileReader fileReader = new FileReader(Path.of(fakeBlob.getTargetDir(), fakeBlob.getBlob()).toFile());

    BeanVerifier<Transaction> verifier = new TransactionVerifier();

    CsvToBeanBuilder<Transaction> builder = new CsvToBeanBuilder<Transaction>(fileReader)
        .withType(Transaction.class)
        .withSeparator(';')
        .withVerifier(verifier)
        .withThrowExceptions(false);

    CsvToBean<Transaction> csvToBean = builder.build();
    Stream<Transaction> readTransaction = csvToBean.stream();

    readTransaction.map(e -> {
        return Stream.of(e);
    }).forEach(e -> {
        deadLetterQueueProcessor.TransactionCheckProcess(e);
    });

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
        assertEquals(1, deadLetterQueueProcessor.getProcessedTrx());
        assertEquals(0,deadLetterQueueProcessor.getExcepitonTrx());
    });
  }

}
