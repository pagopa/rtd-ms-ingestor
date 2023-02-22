package it.gov.pagopa.rtd.ms.rtdmsingestor.repository;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIPair;
import java.util.List;
import java.util.Optional;

public interface IngestorRepository {

  Optional<EPIItem> findItemByHash(String hash);

  List<EPIPair> findItemsByHashs(List<String> hashs);

}
