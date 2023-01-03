package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastracture.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastracture.mongo.PaymentInstrumentItem;

public interface IngestorDAO extends MongoRepository<PaymentInstrumentItem, String> {

    @Query(value = "{ $or :[{'hashPan' : ?0 },{ 'hashPanChildren': ?0 }] , 'state':READY}}", fields = "{ hashPan : 1 }")
    Optional<PaymentInstrumentItem> findItemByHash(String hash);

}
