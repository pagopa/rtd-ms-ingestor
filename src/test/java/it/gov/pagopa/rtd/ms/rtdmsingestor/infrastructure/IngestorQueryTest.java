package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIEntity;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorDAO;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorRepositoryImpl;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataMongoTest
class IngestorQueryTest {

  @Container
  public static final MongoDBContainer mongoContainer = new MongoDBContainer("mongo:4.4.4");

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
  }

  @Value("${ingestor.resources.base.path}")
  String resources;

  @Value("${ingestor.resources.base.path}/tmp")
  String tmpDirectory;

  @Autowired
  private IngestorDAO dao;

  private IngestorRepository repository;

  @Autowired
  MongoTemplate mongoTemplate;

  final EPIEntity paymentInstrumentItem1 = EPIEntity.builder().id("1").hashPan("testHashPan")
      .hashPanChildren(List.of("c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9"))
      .par("par").state("READY").apps(List.of("IDPAY")).network("").issuer("")
      .insertAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
      .insertUser("enrolled_payment_instrument").updateUser("enrolled_payment_instrument")
      .version(1).build();

  final EPIEntity paymentInstrumentItem2 = EPIEntity.builder().id("2")
      .hashPan("abd525b1f1b866145e90f7fff7b47c43ef6b90a6083cd5babfb55332329fce5e").par("par")
      .state("READY").apps(List.of("IDPAY")).network("").issuer("").insertAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now()).insertUser("enrolled_payment_instrument")
      .updateUser("enrolled_payment_instrument").version(1).build();

  final EPIEntity paymentInstrumentItem3 = EPIEntity.builder().id("3")
      .hashPan("ee4bac9851e9f4325b008bd6c92af29d0d45dd6c6511dd286c5995825695feec").par("par")
      .state("REVOKED").apps(List.of("IDPAY")).network("").issuer("").insertAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now()).insertUser("enrolled_payment_instrument")
      .updateUser("enrolled_payment_instrument").version(1).build();

  @BeforeEach
  void setup() {
    mongoTemplate.indexOps("enrolled_payment_instrument")
        .ensureIndex(new Index().on("hashPan", Direction.ASC).unique());

    repository = new IngestorRepositoryImpl(dao);
  }

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
    mongoTemplate.dropCollection(EPIEntity.class);
  }

  @Test
  void testFindHashpanFunction() {
    mongoTemplate.insert(paymentInstrumentItem1);
    mongoTemplate.insert(paymentInstrumentItem2);
    mongoTemplate.insert(paymentInstrumentItem3);

    Assertions.assertTrue(repository
        .findItemByHash("c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9")
        .isPresent());
    Assertions.assertTrue(repository
        .findItemByHash("abd525b1f1b866145e90f7fff7b47c43ef6b90a6083cd5babfb55332329fce5e")
        .isPresent());
    Assertions.assertFalse(repository
        .findItemByHash("7858580aef0faef76c2d6839f84ec383947783966c99cf6afad446a54ddc0e94")
        .isPresent());
    Assertions.assertFalse(repository
        .findItemByHash("ee4bac9851e9f4325b008bd6c92af29d0d45dd6c6511dd286c5995825695feec")
        .isPresent());
  }

}
