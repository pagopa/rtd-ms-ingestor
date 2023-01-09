package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;

public interface IngestorDAO extends MongoRepository<EPIItem, String> {

    //@Query(value = "{ $or :[{'hashPan' : ?0 },{ 'hashPanChildren': ?0 }] , 'state':READY}}")
    @Query(value = "{ $or :[{'hashPan' : ?0 },{ 'hashPanChildren': ?0 }] , 'state':READY}}", fields = "{ hashPan : 1 }")
    Optional<EPIItem> findItemByHash(String hash);

}
