package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class BlobApplicationAwareTest {

  @Value("${ingestor.resources.base.path}")
  String resources;

  @Value("${ingestor.resources.base.path}/tmp")
  String tmpDirectory;

  @MockBean
  CloseableHttpClient closeableHttpClient;

  String containerRtd = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
  String blobNameRtd = "CSTAR.99910.TRNLOG.20220316.164707.001.csv.pgp.decrypted";

  BlobApplicationAware fakeBlob;

  @BeforeEach
  void setUp() throws IOException {
    //Create dummy files to be deleted
    File blobFile = Path.of(tmpDirectory, blobNameRtd).toFile();
    blobFile.getParentFile().mkdirs();
    blobFile.createNewFile();

    FileOutputStream blobStream = new FileOutputStream(
        Path.of(tmpDirectory, blobNameRtd).toString());

    //Instantiate a fake blob with empty content
    fakeBlob = new BlobApplicationAware(
        "/blobServices/default/containers/" + containerRtd + "/blobs/" + blobNameRtd);
    fakeBlob.setTargetDir(tmpDirectory);
  }

  @AfterEach
  void cleanTmpFiles() throws IOException {
    FileUtils.deleteDirectory(Path.of(tmpDirectory).toFile());
  }


  @Test
  void shouldMatchRegexRtd() {
    assertSame(Status.RECEIVED, fakeBlob.getStatus());
  }

  @Test
  void shouldNotMatchRegexEventNotOfInterest(CapturedOutput output) {
    String containerAde = "ade-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blobAde = "ADE.99910.TRNLOG.20220228.203107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + containerAde + "/blobs/" + blobAde;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(Status.INIT, myBlob.getStatus());
    assertThat(output.getOut(), containsString("Event not of interest:"));
  }

  //Ingestor do not accept ADE files
  @Test
  void shouldNotMatchRegexWrogName(CapturedOutput output) {
    String blobAde = "ADE.99910.TRNLOG.20220228.203107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + containerRtd + "/blobs/" + blobAde;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(Status.INIT, myBlob.getStatus());
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  //The test parameters reproduce the following scenarios: blobUriShouldFailWrongService,
  // blobUriShouldFailNoService, blobUriShouldFailShortABI, blobUriShouldFailLongABI,
  // blobUriShouldFailNoABI, blobUriShouldFailWrongFiletype, blobUriShouldFailNoFiletype,
  // blobUriShouldFailWrongDate, blobUriShouldFailNoDate, blobUriShouldFailWrongTime,
  // blobUriShouldFailNoTime, blobUriShouldFailWrongProgressive,
  // blobUriShouldFailNoProgressive
  @ParameterizedTest
  @ValueSource(strings = {"CSTA.99910.TRNLOG.20220228.203107.001.csv.pgp",
      ".99910.TRNLOG.20220228.203107.001.csv.pgp", "CSTAR.9991.TRNLOG.20220228.203107.001.csv.pgp",
      "CSTAR.999100.TRNLOG.20220228.203107.999.csv.pgp",
      "CSTAR..TRNLOG.20220228.203107.001.csv.pgp", "CSTAR.99910.TRNLO.20220228.203107.001.csv.pgp",
      "CSTAR.99910..20220228.203107.001.csv.pgp", "CSTAR.99910.TRNLOG.20220230.103107.001.csv.pgp",
      "CSTAR.99910.TRNLOG..103107.001.csv.pgp", "CSTAR.99910.TRNLOG.20220228.243107.001.csv.pgp",
      "CSTAR.99910.TRNLOG.20220228..001.csv.pgp", "CSTAR.99910.TRNLOG.20220228.103107.1.csv.pgp",
      "CSTAR.99910.TRNLOG.20220228.103107..csv.pgp"})
  void blobUriShouldFailRegex(String blobName, CapturedOutput output) {
    String blobUri = "/blobServices/default/containers/" + containerRtd + "/blobs/" + blobName;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(Status.INIT, myBlob.getStatus());
  }

  @Test
  void shouldCleanLocalFiles() {
    assertEquals(Status.LOCALLY_DELETED, fakeBlob.localCleanup().getStatus());
    assertFalse(Files.exists(Path.of(tmpDirectory, fakeBlob.getBlob())));
  }

  //This test simulates the scenario where in the temporary folder with the same name as the blob
  // to be deleted.
  //Thi is done in order to trigger the catch clause in the localCleanup method.
  @Test
  void shouldFailFindingLocalEncryptedFile(CapturedOutput output) throws IOException {
    File nestedBlob = Path.of(tmpDirectory, blobNameRtd + ".dir", blobNameRtd + ".nested").toFile();
    nestedBlob.getParentFile().mkdirs();
    nestedBlob.createNewFile();
    assertNotEquals(Status.LOCALLY_DELETED, fakeBlob.localCleanup().getStatus());
    assertThat(output.getOut(), containsString("Failed to delete local file:"));
  }

}
