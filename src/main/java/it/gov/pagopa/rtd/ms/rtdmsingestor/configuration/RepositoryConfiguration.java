package it.gov.pagopa.rtd.ms.rtdmsingestor.configuration;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorDAO;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorRepositoryImpl;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
    basePackages = "it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories")
@Slf4j
public class RepositoryConfiguration {

  @Value("#{new Boolean(${ingestor.anonymizePaymentInstrument})}")
  private Boolean anonymizePaymentInstrument;

  @Bean
  public IngestorRepository ingestorRepository(
      IngestorDAO ingestorDAO
  ) {
    if (anonymizePaymentInstrument) {
      return new IngestorRepositoryImpl(ingestorDAO);
    } else {
      log.warn("Anonymize payment instrument disabled");
      return hash -> Optional.of(EPIItem.builder().hashPan(hash).build());
    }
  }
}
