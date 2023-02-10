package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import java.util.Optional;

public class IngestorRepositoryImpl implements IngestorRepository {

  private final IngestorDAO dao;

  public IngestorRepositoryImpl(IngestorDAO dao) {
    this.dao = dao;
  }

  @Override
  public Optional<EPIItem> findItemByHash(String hash) {
    return dao.findItemByHash(hash);
  }
}
