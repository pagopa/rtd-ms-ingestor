package it.gov.pagopa.rtd.ms.rtdmsingestor.repository;

import java.util.Optional;
import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.PaymentInstrumentItem;

public interface IngestorRepository {

    Optional<PaymentInstrumentItem> findItemByHash(String hash);

}
