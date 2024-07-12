package it.gov.pagopa.rtd.ms.rtdmsingestor.service.rtd;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo.EPIItem;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class RtdEventProcessorHandlerTest {

    private final String resources = "src/test/resources";
    private final String tmpDirectory = resources + "/tmp";
    private final String containerRtd = "rtd-transactions-decrypted";
    private final String blobNameRtd = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp.0.decrypted";

    private final BlobApplicationAware fakeBlobRtd = new BlobApplicationAware(
            "/blobServices/default/containers/" + containerRtd + "/blobs/" + blobNameRtd);


    private IngestorRepository repository;
    private RtdEventProcessorHandler rtdEventProcessorHandler;

    @BeforeEach
    void setup() {
        repository = mock(IngestorRepository.class);
        rtdEventProcessorHandler = new RtdEventProcessorHandler(
                mock(StreamBridge.class),
                repository
        );
    }

    @AfterEach
    void cleanTmpFiles() throws IOException {
        FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
    }

    @Test
    void shouldProcess() throws IOException {
        final String transactions = "testTransactions.csv";

        when(repository.findItemByHash(any())).thenReturn(Optional.of(EPIItem.builder()
                .hashPan("b50245d5fee9fa11bead50e7d0afb6c269c77f59474a87442f867ba9643021fc").build()));

        // Create fake file to process
        File decryptedFile = Path.of(tmpDirectory, blobNameRtd).toFile();
        decryptedFile.getParentFile().mkdirs();
        decryptedFile.createNewFile();

        FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobNameRtd).toString());
        Files.copy(Path.of(resources, transactions), blobDst);

        fakeBlobRtd.setTargetDir(tmpDirectory);
        fakeBlobRtd.setStatus(BlobApplicationAware.Status.DOWNLOADED);

        rtdEventProcessorHandler.processEvent(fakeBlobRtd);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertEquals(rtdEventProcessorHandler.getNumTotalTrx(), rtdEventProcessorHandler.getNumCorrectTrx());
            assertEquals(5, rtdEventProcessorHandler.getNumTotalTrx());
            assertEquals(5, rtdEventProcessorHandler.getNumCorrectTrx());
            assertEquals(BlobApplicationAware.Status.PROCESSED, fakeBlobRtd.getStatus());
        });
    }

    @Test
    void shouldNotProcessForMissingFile(CapturedOutput output) {

        fakeBlobRtd.setTargetDir(resources);
        fakeBlobRtd.setStatus(BlobApplicationAware.Status.DOWNLOADED);
        fakeBlobRtd.setBlob(blobNameRtd + ".missing");

        rtdEventProcessorHandler.processEvent(fakeBlobRtd);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(output.getOut(), containsString("Extracting transactions from:"));
            assertThat(output.getOut(), containsString("Missing blob file:"));
            assertThat(output.getOut(), not(containsString("Extracted")));
            assertNotEquals(BlobApplicationAware.Status.PROCESSED, fakeBlobRtd.getStatus());
        });
    }

    // This test uses a file with all malformed transaction
    // There is one malformed transaction for every field in the object Transaction.
    @Test
    void shouldNotProcessForMalformedFields(CapturedOutput output) throws IOException {
        final String transactions = "testMalformedTransactions.csv";

        when(repository.findItemByHash(any())).thenReturn(Optional.of(EPIItem.builder()
                .hashPan("c3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9").build()));

        // Create fake file to process
        File decryptedFile = Path.of(tmpDirectory, blobNameRtd).toFile();
        decryptedFile.getParentFile().mkdirs();
        decryptedFile.createNewFile();
        FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobNameRtd).toString());
        Files.copy(Path.of(resources, transactions), blobDst);

        fakeBlobRtd.setTargetDir(tmpDirectory);
        fakeBlobRtd.setStatus(BlobApplicationAware.Status.DOWNLOADED);

        rtdEventProcessorHandler.processEvent(fakeBlobRtd);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

            assertEquals(3, rtdEventProcessorHandler.getNumCorrectTrx());
            assertEquals(0, rtdEventProcessorHandler.getNumNotEnrolledCards());
            assertEquals(53, rtdEventProcessorHandler.getNumTotalTrx());

            assertThat(output.getOut(), containsString("Invalid character for Fiscal Code "));
            assertThat(output.getOut(), containsString("Invalid length for Fiscal Code "));
            assertThat(output.getOut(), containsString("Invalid checksum for Fiscal Code "));
            assertEquals(BlobApplicationAware.Status.PROCESSED, fakeBlobRtd.getStatus());
        });
    }

    // This test uses a file with all malformed transaction
    // There is one malformed transaction for every field in the object Transaction.
    @ParameterizedTest
    @CsvSource({"testMalformedTransactionHash.csv,",
            "testMalformedTransactionHash_2.csv,3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9",
            "testMalformedTransactionHash_3.csv,ac3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9",
            "testMalformedTransactionHash_4.csv,+3141e7c87d0bf7faac1ea3c79b2312279303b87781eedbb47ec8892f63df3e9"})
    void shouldNotProcessForMalformedEmptyHashPan(String fileName, String hashpan)
            throws IOException {

        final String transactions = fileName;

        when(repository.findItemByHash(any()))
                .thenReturn(Optional.of(EPIItem.builder().hashPan(hashpan).build()));

        // Create fake file to process
        File decryptedFile = Path.of(tmpDirectory, blobNameRtd).toFile();
        decryptedFile.getParentFile().mkdirs();
        decryptedFile.createNewFile();
        FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobNameRtd).toString());
        Files.copy(Path.of(resources, transactions), blobDst);

        fakeBlobRtd.setTargetDir(tmpDirectory);
        fakeBlobRtd.setStatus(BlobApplicationAware.Status.DOWNLOADED);

        rtdEventProcessorHandler.processEvent(fakeBlobRtd);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertEquals(0, rtdEventProcessorHandler.getNumCorrectTrx());
            assertEquals(1, rtdEventProcessorHandler.getNumTotalTrx());
            assertEquals(0, rtdEventProcessorHandler.getNumNotEnrolledCards());
            assertEquals(BlobApplicationAware.Status.PROCESSED, fakeBlobRtd.getStatus());
        });
    }

    @Test
    void shouldNotFailOnEmptyFile() throws IOException {
        final String transactions = "testEmptyFile.csv";

        when(repository.findItemByHash(any())).thenReturn(Optional.of(EPIItem.builder()
                .hashPan("b50245d5fee9fa11bead50e7d0afb6c269c77f59474a87442f867ba9643021fc").build()));

        File decryptedFile = Path.of(tmpDirectory, blobNameRtd).toFile();
        decryptedFile.getParentFile().mkdirs();
        decryptedFile.createNewFile();
        FileOutputStream blobDst = new FileOutputStream(Path.of(tmpDirectory, blobNameRtd).toString());
        Files.copy(Path.of(resources, transactions), blobDst);

        fakeBlobRtd.setTargetDir(tmpDirectory);
        fakeBlobRtd.setStatus(BlobApplicationAware.Status.DOWNLOADED);

        rtdEventProcessorHandler.processEvent(fakeBlobRtd);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertEquals(0, rtdEventProcessorHandler.getNumCorrectTrx());
        });
    }


}