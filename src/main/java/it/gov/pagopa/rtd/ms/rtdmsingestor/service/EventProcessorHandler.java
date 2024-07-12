package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;

import java.nio.file.Path;

public interface EventProcessorHandler {
    BlobApplicationAware processEvent(BlobApplicationAware blob);
}
