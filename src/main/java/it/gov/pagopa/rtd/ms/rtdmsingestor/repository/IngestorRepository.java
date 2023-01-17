package it.gov.pagopa.rtd.ms.rtdmsingestor.repository;

import java.util.Optional;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;

public interface IngestorRepository {

    Optional<EPIItem> findItemByHash(String hash);

}
