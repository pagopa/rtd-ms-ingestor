package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.apache.commons.io.FileUtils;

import it.gov.pagopa.rtd.ms.rtdmsingestor.configs.MongoDbTest;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.RepositoryConfiguration;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.PaymentInstrumentItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorDAO;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorRepositoryImpl;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.BlobRestConnector;


@RunWith(SpringRunner.class)
@MongoDbTest
@TestPropertySource(value = {"classpath:application-test.yml"}, inheritProperties = false)
@Import(RepositoryConfiguration.class)
class IngestorQueryTest {

    @Value("${ingestor.resources.base.path}")
    String resources;

    @Value("${ingestor.resources.base.path}/tmp")
    String tmpDirectory;

    @Autowired
    private IngestorDAO dao;

    private IngestorRepository repository;


    private final String container = "rtd-transactions-decrypted";
    private final String blobName = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp.0.decrypted";

    private BlobApplicationAware fakeBlob = new BlobApplicationAware(
        "/blobServices/default/containers/" + container + "/blobs/" + blobName);

    final PaymentInstrumentItem paymentInstrumentItem = PaymentInstrumentItem
        .builder()
        .id("")
        .hashPan("c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9")
        .par("par")
        .state("READY")
        .apps(List.of("IDPAY"))
        .network("")
        .issuer("")
        .insertAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .insertUser("enrolled_payment_instrument")
        .updateUser("enrolled_payment_instrument")
        .version(1)
        .build();


    @BeforeEach
    void setup(@Autowired MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps("enrolled_payment_instrument")
                .ensureIndex(new Index().on("hashPan", Direction.ASC).unique());

        mongoTemplate.insert(paymentInstrumentItem);

        repository = new IngestorRepositoryImpl(dao);
    }

    @AfterEach
    void cleanTmpFiles(@Autowired MongoTemplate mongoTemplate) throws IOException {
        FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
        mongoTemplate.dropCollection(PaymentInstrumentItem.class);
    }

    @Test
    void testHashReplacement() throws IOException{
        String transactions = "testHashReplacement.csv";
        //Create fake file to process
        File decryptedFile = Path.of(tmpDirectory, blobName).toFile();
        decryptedFile.getParentFile().mkdirs();
        decryptedFile.createNewFile();

        FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobName).toString());
        Files.copy(Path.of(resources, transactions), blobDst);


        fakeBlob.setTargetDir(tmpDirectory);
        fakeBlob.setStatus(BlobApplicationAware.Status.DOWNLOADED);

        assertEquals(true,repository.findItemByHash("c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9").isPresent());

    }
}
