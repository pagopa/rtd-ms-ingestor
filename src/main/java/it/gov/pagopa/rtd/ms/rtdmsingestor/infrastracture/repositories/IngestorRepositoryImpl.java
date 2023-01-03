package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastracture.repositories;
import java.util.Optional;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastracture.mongo.PaymentInstrumentItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class IngestorRepositoryImpl implements IngestorRepository{

    private final IngestorDAO dao;

    public IngestorRepositoryImpl(IngestorDAO dao){
        this.dao = dao;
    }

    @Override
    public Optional<PaymentInstrumentItem> findItemByHash(String hash) {
        return dao.findItemByHash(hash);
    }

}
