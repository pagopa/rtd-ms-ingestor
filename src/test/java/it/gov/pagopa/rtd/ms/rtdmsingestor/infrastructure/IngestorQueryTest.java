package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import org.apache.commons.io.FileUtils;

import it.gov.pagopa.rtd.ms.rtdmsingestor.configs.MongoDbTest;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.RepositoryConfiguration;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIEntity;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorDAO;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorRepositoryImpl;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;

@MongoDbTest
@Import(RepositoryConfiguration.class)
class IngestorQueryTest {

        @Value("${ingestor.resources.base.path}")
        String resources;

        @Value("${ingestor.resources.base.path}/tmp")
        String tmpDirectory;

        @Autowired
        private IngestorDAO dao;

        private IngestorRepository repository;

        final EPIEntity paymentInstrumentItem_1 = EPIEntity
                        .builder()
                        .id("1")
                        .hashPan("testHashPan")
                        .hashPanChildren(List.of("c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9"))
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

        final EPIEntity paymentInstrumentItem_2 = EPIEntity
                        .builder()
                        .id("2")
                        .hashPan("abd525b1f1b866145e90f7fff7b47c43ef6b90a6083cd5babfb55332329fce5e")
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

        final EPIEntity paymentInstrumentItem_3 = EPIEntity
                        .builder()
                        .id("3")
                        .hashPan("ee4bac9851e9f4325b008bd6c92af29d0d45dd6c6511dd286c5995825695feec")
                        .par("par")
                        .state("REVOKED")
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

                mongoTemplate.insert(paymentInstrumentItem_1);
                mongoTemplate.insert(paymentInstrumentItem_2);
                mongoTemplate.insert(paymentInstrumentItem_3);

                repository = new IngestorRepositoryImpl(dao);
        }

        @AfterEach
        void cleanTmpFiles(@Autowired MongoTemplate mongoTemplate) throws IOException {
                FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
                mongoTemplate.dropCollection(EPIEntity.class);
        }

        @Test
        void testFindHashpanFunction() throws IOException {
                /* assertEquals(true,
                                repository.findItemByHash(
                                                "c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9")
                                                .isPresent());
                assertEquals(true,
                                repository.findItemByHash(
                                                "abd525b1f1b866145e90f7fff7b47c43ef6b90a6083cd5babfb55332329fce5e")
                                                .isPresent());
                assertEquals(false, repository
                                .findItemByHash("7858580aef0faef76c2d6839f84ec383947783966c99cf6afad446a54ddc0e94")
                                .isPresent());
                assertEquals(false, repository
                                .findItemByHash("ee4bac9851e9f4325b008bd6c92af29d0d45dd6c6511dd286c5995825695feec")
                                .isPresent()); */
        }

}
