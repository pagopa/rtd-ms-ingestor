package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.junit4.SpringRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;

import it.gov.pagopa.rtd.ms.rtdmsingestor.TestUtils;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configs.MongoDbTest;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.RepositoryConfiguration;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.PaymentInstrumentItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorDAO;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorRepositoryImpl;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;


@RunWith(SpringRunner.class)
@MongoDbTest
@Import(RepositoryConfiguration.class)
class  IngestorRepositoryTest {

  private static final String TEST_HASH_PAN = TestUtils.generateRandomHashPan();
  private static final String TEST_CHILD_HASH_PAN = TestUtils.generateRandomHashPan();
  private static final String TEST_PAR = "par";

  @Autowired
  private IngestorDAO dao;

  @Autowired
  private IngestorRepository repository;

  @BeforeEach
  void setup(@Autowired MongoTemplate mongoTemplate) {
    mongoTemplate.indexOps("enrolled_payment_instrument")
            .ensureIndex(new Index().on("hashPan", Direction.ASC).unique());

    repository = new IngestorRepositoryImpl(dao);
  }

  @AfterEach
  void clean(@Autowired MongoTemplate mongoTemplate) {
    mongoTemplate.dropCollection(PaymentInstrumentItem.class);
  }

}
