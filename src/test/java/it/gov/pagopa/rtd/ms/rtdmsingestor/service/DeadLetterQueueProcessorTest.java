package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mongodb.MongoException;
import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorDAO;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@Import({TestChannelBinderConfiguration.class})
@EnableAutoConfiguration(exclude = {
    MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@TestPropertySource(value = {"classpath:application-test.yml"}, inheritProperties = false)
class DeadLetterQueueProcessorTest {

  @Value("${ingestor.resources.base.path}")
  String resources;

  @Value("${ingestor.resources.base.path}/tmp")
  String tmpDirectory;

  @SpyBean
  private BlobRestConnector blobRestConnector;

  @SpyBean
  private DeadLetterQueueProcessor deadLetterQueueProcessor;

  @MockBean
  IngestorRepository repository;
  @MockBean
  IngestorDAO dao;
  @MockBean
  StreamBridge streamBridge;

  private final String container = "rtd-transactions-decrypted";
  private final String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp.0.decrypted";

  private final BlobApplicationAware fakeBlob = new BlobApplicationAware(
      "/blobServices/default/containers/" + container + "/blobs/" + blobName);

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }

  @Test
  void mongoQueryError() throws IOException {
    final String transactions = "testHashReplacement.csv";

    when(repository.findItemByHash(any())).thenThrow(new MongoException(""));

    // Create fake file to process
    File decryptedFile = Path.of(tmpDirectory, blobName).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();

    FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobName).toString());
    Files.copy(Path.of(resources, transactions), blobDst);

    fakeBlob.setTargetDir(tmpDirectory);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    FileReader fileReader =
        new FileReader(Path.of(fakeBlob.getTargetDir(), fakeBlob.getBlob()).toFile());

    BeanVerifier<Transaction> verifier = new TransactionVerifier();

    CsvToBeanBuilder<Transaction> builder =
        new CsvToBeanBuilder<Transaction>(fileReader).withType(Transaction.class).withSeparator(';')
            .withVerifier(verifier).withThrowExceptions(false);

    CsvToBean<Transaction> csvToBean = builder.build();
    Stream<Transaction> readTransaction = csvToBean.stream();

    deadLetterQueueProcessor.transactionCheckProcess(readTransaction);

    assertEquals(0, deadLetterQueueProcessor.getProcessedTrx());
    assertEquals(1, deadLetterQueueProcessor.getExceptionTrx());
  }

  @Test
  void deadLetterQueueCorrectProcessing() throws IOException {
    final String transactions = "testHashReplacement.csv";

    when(repository.findItemByHash(any())).thenReturn(Optional.of(EPIItem.builder()
        .hashPan("b50245d5fee9fa11bead50e7d0afb6c269c77f59474a87442f867ba9643021fc").build()));

    // Create fake file to process
    File decryptedFile = Path.of(tmpDirectory, blobName).toFile();
    decryptedFile.getParentFile().mkdirs();
    decryptedFile.createNewFile();

    FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobName).toString());
    Files.copy(Path.of(resources, transactions), blobDst);

    fakeBlob.setTargetDir(tmpDirectory);
    fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    FileReader fileReader =
        new FileReader(Path.of(fakeBlob.getTargetDir(), fakeBlob.getBlob()).toFile());

    BeanVerifier<Transaction> verifier = new TransactionVerifier();

    CsvToBeanBuilder<Transaction> builder =
        new CsvToBeanBuilder<Transaction>(fileReader).withType(Transaction.class).withSeparator(';')
            .withVerifier(verifier).withThrowExceptions(false);

    CsvToBean<Transaction> csvToBean = builder.build();
    Stream<Transaction> readTransaction = csvToBean.stream();

    deadLetterQueueProcessor.transactionCheckProcess(readTransaction);

    assertEquals(1, deadLetterQueueProcessor.getProcessedTrx());
    assertEquals(0, deadLetterQueueProcessor.getExceptionTrx());
  }
}
