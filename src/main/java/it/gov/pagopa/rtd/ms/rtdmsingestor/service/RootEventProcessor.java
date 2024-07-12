package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Application;

import java.nio.file.Path;

import it.gov.pagopa.rtd.ms.rtdmsingestor.service.rtd.RtdEventProcessorHandler;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.wallet.WalletEventProcessorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
public class RootEventProcessor {

  private final WalletEventProcessorHandler walletEventProcessorHandler;
  private final RtdEventProcessorHandler rtdEventProcessorHandler;

    public RootEventProcessor(
            WalletEventProcessorHandler walletEventProcessorHandler,
            RtdEventProcessorHandler rtdEventProcessorHandler
    ) {
        this.walletEventProcessorHandler = walletEventProcessorHandler;
        this.rtdEventProcessorHandler = rtdEventProcessorHandler;
    }

    public BlobApplicationAware process(BlobApplicationAware blob) {
    if (Application.RTD.equals(blob.getApp())) {
      return rtdEventProcessorHandler.processEvent(blob);
    } else if (Application.WALLET.equals(blob.getApp())) {
      return walletEventProcessorHandler.processEvent(blob);
    }
    return blob;
  }
}
