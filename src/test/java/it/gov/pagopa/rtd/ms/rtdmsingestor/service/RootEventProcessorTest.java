package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.rtd.RtdEventProcessorHandler;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.wallet.WalletEventProcessorHandler;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RootEventProcessorTest {

  private final String containerRtd = "rtd-transactions-decrypted";
  private final String blobNameRtd = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp.0.decrypted";

  private final String containerWallet = "wallet-contracts-decrypted";
  private final String blobNameWallet = "WALLET.CONTRACTS.20240313.174811.001.json.pgp.0.decrypted";

  private WalletEventProcessorHandler walletEventProcessorHandler = mock(WalletEventProcessorHandler.class);
  private RtdEventProcessorHandler rtdEventProcessorHandler = mock(RtdEventProcessorHandler.class);

  private RootEventProcessor rootEventProcessor = new RootEventProcessor(
          walletEventProcessorHandler,
           rtdEventProcessorHandler
  );

  @Test
  void shouldProcessRtdEventWithProperHandler() {
    final var rtdBlobEvent = new BlobApplicationAware(
            "/blobServices/default/containers/" + containerRtd + "/blobs/" + blobNameRtd);

    doReturn(rtdBlobEvent).when(rtdEventProcessorHandler).processEvent(any());
    rootEventProcessor.process(rtdBlobEvent);
    verify(rtdEventProcessorHandler, times(1)).processEvent(eq(rtdBlobEvent));
  }

  @Test
  void shouldProcessWalletEventWithProperHandler() {
    final var walletBlobEvent = new BlobApplicationAware(
            "/blobServices/default/containers/" + containerWallet + "/blobs/" + blobNameWallet);

    doReturn(walletBlobEvent).when(walletEventProcessorHandler).processEvent(any());
    rootEventProcessor.process(walletBlobEvent);
    verify(walletEventProcessorHandler, times(1)).processEvent(eq(walletBlobEvent));
  }
}
