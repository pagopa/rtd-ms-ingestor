package it.gov.pagopa.rtd.ms.rtdmsingestor.repository;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import java.util.Optional;

public interface IngestorRepository {
  Optional<EPIItem> findItemByHash(String hash);
}
