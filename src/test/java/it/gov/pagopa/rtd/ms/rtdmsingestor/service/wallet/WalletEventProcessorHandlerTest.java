package it.gov.pagopa.rtd.ms.rtdmsingestor.service.wallet;

import it.gov.pagopa.rtd.ms.rtdmsingestor.adapter.ContractAdapter;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.properties.WalletConfigurationProperties;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.ContractMethodAttributes;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.BlobRestConnector;
import it.gov.pagopa.rtd.ms.rtdmsingestor.utils.Anonymizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

class WalletEventProcessorHandlerTest {

    private final String resources = "src/test/resources";
    private final String tmpDirectory = resources + "/tmp";
    private final String containerWallet = "wallet-contracts-decrypted";
    private final String blobNameWallet = "WALLET.CONTRACTS.20240313.174811.001.json.pgp.0.decrypted";
    private final String blobNameWalletMalformed = "WALLET.CONTRACTS.20240402.103010.001.json.pgp.0.decrypted";

    private final BlobApplicationAware fakeBlobWallet = new BlobApplicationAware(
            "/blobServices/default/containers/" + containerWallet + "/blobs/" + blobNameWallet);

    private final BlobApplicationAware fakeBlobWalletMalformed = new BlobApplicationAware(
            "/blobServices/default/containers/" + containerWallet + "/blobs/" + blobNameWalletMalformed);

    private BlobRestConnector connector;
    private WalletService walletService;
    private WalletEventProcessorHandler walletEventProcessorHandler;

    @BeforeEach
    void setup() {
        final var configuration = new WalletConfigurationProperties(
                "url",
                "apiKey",
                "update",
                "delete",
                1, 1,1, 1, 1, 1, 1, 1, 1, 1, 1
        );
        connector = Mockito.mock(BlobRestConnector.class);
        walletService = Mockito.mock(WalletService.class);
        walletEventProcessorHandler = new WalletEventProcessorHandler(
                walletService,
                new ContractAdapter(),
                new Anonymizer("123"),
                configuration
        );
    }

    @Test
    void shouldProcessWalletEvent() throws IOException {

        // Create fake file to process
        File decryptedFile = Path.of(tmpDirectory, blobNameWallet).toFile();
        decryptedFile.getParentFile().mkdirs();
        decryptedFile.createNewFile();

        FileOutputStream blobDst = new FileOutputStream(
                Path.of(tmpDirectory, blobNameWallet).toString());
        Files.copy(Path.of(resources, blobNameWallet), blobDst);

        fakeBlobWallet.setTargetDir(tmpDirectory);
        fakeBlobWallet.setStatus(BlobApplicationAware.Status.DOWNLOADED);

        doReturn(true).when(walletService)
                .postContract(any(ContractMethodAttributes.class), any(String.class));
        doReturn(true).when(walletService).deleteContract(any(String.class), any(String.class));

        walletEventProcessorHandler.processEvent(fakeBlobWallet);
        await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
            assertEquals(BlobApplicationAware.Status.PROCESSED, fakeBlobWallet.getStatus());
        });
    }

    @Test
    void shouldNotProcessWalletEventFailedRequest() throws IOException {

        // Create fake file to process
        File decryptedFile = Path.of(tmpDirectory, blobNameWalletMalformed).toFile();
        decryptedFile.getParentFile().mkdirs();
        decryptedFile.createNewFile();

        FileOutputStream blobDst = new FileOutputStream(
                Path.of(tmpDirectory, blobNameWallet).toString());
        Files.copy(Path.of(resources, blobNameWallet), blobDst);

        fakeBlobWallet.setTargetDir(tmpDirectory);
        fakeBlobWallet.setStatus(BlobApplicationAware.Status.DOWNLOADED);

        doReturn(false).when(walletService)
                .postContract(any(ContractMethodAttributes.class), any(String.class));

        walletEventProcessorHandler.processEvent(fakeBlobWallet);
        await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
            assertEquals(BlobApplicationAware.Status.PROCESSED, fakeBlobWallet.getStatus());
        });
    }

    @Test
    void shouldNotProcessWalletEventMalformedContracts() throws IOException {

        // Create fake file to process
        File decryptedFile = Path.of(tmpDirectory, blobNameWalletMalformed).toFile();
        decryptedFile.getParentFile().mkdirs();
        decryptedFile.createNewFile();

        FileOutputStream blobDst = new FileOutputStream(
                Path.of(tmpDirectory, blobNameWalletMalformed).toString());
        Files.copy(Path.of(resources, blobNameWalletMalformed), blobDst);

        fakeBlobWalletMalformed.setTargetDir(tmpDirectory);
        fakeBlobWalletMalformed.setStatus(BlobApplicationAware.Status.DOWNLOADED);

        doReturn(false).when(walletService)
                .postContract(any(ContractMethodAttributes.class), any(String.class));

        walletEventProcessorHandler.processEvent(fakeBlobWalletMalformed);
        await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
            assertEquals(BlobApplicationAware.Status.PROCESSED, fakeBlobWalletMalformed.getStatus());
        });
    }

}