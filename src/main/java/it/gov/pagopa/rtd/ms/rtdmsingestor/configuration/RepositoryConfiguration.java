package it.gov.pagopa.rtd.ms.rtdmsingestor.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorDAO;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorRepositoryImpl;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;

@Configuration
@EnableMongoRepositories(basePackages = "it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories")
public class RepositoryConfiguration {

    @Bean
    public IngestorRepository ingestorRepository(IngestorDAO ingestorDAO) {
        return new IngestorRepositoryImpl(ingestorDAO);
    }

}
