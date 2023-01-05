package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.PaymentInstrumentItem;

public interface IngestorDAO extends MongoRepository<PaymentInstrumentItem, String> {

    @Query(value = "{ $or :[{'hashPan' : ?0 },{ 'hashPanChildren': ?0 }] , 'state':READY}}")
    Optional<PaymentInstrumentItem> findItemByHash(String hash);

}
