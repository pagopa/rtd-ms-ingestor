package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import org.apache.http.impl.client.CloseableHttpClient;
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

  @MockBean
  CloseableHttpClient closeableHttpClient;

  @Test
  void shouldMatchRegexRtd() {
    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(Status.RECEIVED, myBlob.getStatus());
  }

  @Test
  void shouldNotMatchRegexEventNotOfInterest(CapturedOutput output) {
    String container = "ade-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "ADE.99910.TRNLOG.20220228.203107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(Status.INIT, myBlob.getStatus());
    assertThat(output.getOut(), containsString("Event not of interest:"));
  }

  @Test
  void shouldNotMatchRegexConflictAppAndName() {
    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blob = "ADE.99910.TRNLOG.20220228.203107.001.csv.pgp";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(Status.INIT, myBlob.getStatus());
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
    String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
    String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blobName;
    BlobApplicationAware myBlob = new BlobApplicationAware(blobUri);
    assertSame(Status.INIT, myBlob.getStatus());
  }

}
