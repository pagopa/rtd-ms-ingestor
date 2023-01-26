package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories;

import java.util.Optional;
import java.util.Random;

import com.mongodb.MongoException;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;

public class IngestorRepositoryImpl implements IngestorRepository {

    private final IngestorDAO dao;
    private Random random;

    public IngestorRepositoryImpl(IngestorDAO dao) {
        this.dao = dao;
        this.random = new Random();
    }

    @Override
    public Optional<EPIItem> findItemByHash(String hash) {
        MongoException ex = new MongoException("Error 429");
        if(random.nextDouble()<0.5){
            throw ex;
        }else{
            return dao.findItemByHash(hash);
        }
    }

}
