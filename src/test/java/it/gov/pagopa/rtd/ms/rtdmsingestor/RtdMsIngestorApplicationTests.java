package it.gov.pagopa.rtd.ms.rtdmsingestor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.repositories.IngestorDAO;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsingestor.repository.IngestorRepository;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.BlobRestConnector;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.RootEventProcessor;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@TestPropertySource(value = {"classpath:application-test.yml"}, inheritProperties = false)
@ExtendWith(OutputCaptureExtension.class)
@Import(TestChannelBinderConfiguration.class)
class RtdMsIngestorApplicationTests {

  @Autowired
  private InputDestination inputDestination;

  @MockBean
  private BlobApplicationAware blobApplicationAware;

  @SpyBean
  private BlobRestConnector blobRestConnector;

  @SpyBean
  private RootEventProcessor blobProcessor;

  @MockBean
  CloseableHttpClient client;

  @MockBean
  IngestorRepository repository;

  @MockBean
  IngestorDAO ingestorDAO;

  private final String container = "rtd-transactions-decrypted";
  private final String blob = "CSTAR.99910.TRNLOG.20220316.103107.001.csv.pgp";
  private final String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;

  private final String myId = "my_id";
  private final String myTopic = "my_topic";
  private final String myEventType = "Microsoft.Storage.BlobCreated";

  EventGridEvent myEvent;
  List<EventGridEvent> myList;

  private BlobApplicationAware blobReceived;
  private BlobApplicationAware blobDownloaded;
  private BlobApplicationAware blobProcessed;
  private BlobApplicationAware blobLocallyDeleted;

  @BeforeEach
  void setUp() {
    myEvent = new EventGridEvent();
    myEvent.setId(myId);
    myEvent.setTopic(myTopic);
    myEvent.setEventType(myEventType);
    myEvent.setSubject(blobUri);
    myList = new ArrayList<>();
    myList.add(myEvent);

    blobReceived = new BlobApplicationAware(blobUri);
    blobDownloaded = new BlobApplicationAware(blobUri);
    blobProcessed = new BlobApplicationAware(blobUri);
    blobLocallyDeleted = new BlobApplicationAware(blobUri);

    blobReceived.setStatus(Status.RECEIVED);
    blobDownloaded.setStatus(Status.DOWNLOADED);
    blobProcessed.setStatus(Status.PROCESSED);
    blobLocallyDeleted.setStatus(Status.LOCALLY_DELETED);
  }

  @Test
  void shouldConsumeMessage() {

    // Mock every step of the message handling
    doReturn(blobDownloaded).when(blobRestConnector).get(any(BlobApplicationAware.class));
    doReturn(blobProcessed).when(blobProcessor).process(any(BlobApplicationAware.class));
    doReturn(blobApplicationAware).when(blobRestConnector)
        .deleteRemote(any(BlobApplicationAware.class));

    // Mock of the interested blob's methods
    doReturn(blobLocallyDeleted).when(blobApplicationAware).localCleanup();
    doReturn(Status.REMOTELY_DELETED).when(blobApplicationAware).getStatus();

    inputDestination.send(MessageBuilder.withPayload(myList).build());

    verify(blobRestConnector, times(1)).get(any());
    verify(blobProcessor, times(1)).process(any());
    verify(blobRestConnector, times(1)).deleteRemote(any());
    verify(blobApplicationAware, times(1)).localCleanup();

  }

  @Test
  void shouldFilterMessageForWrongService(CapturedOutput output) {

    // Set wrong blob name
    myEvent.setSubject("/blobServices/default/containers/" + container
        + "/blobs/ADE.99910.TRNLOG.20220228.103107.001.csv.pgp");

    inputDestination.send(MessageBuilder.withPayload(myList).build());

    verify(blobRestConnector, times(0)).get(any());
    verify(blobProcessor, times(0)).process(any());
    verify(blobRestConnector, times(0)).deleteRemote(any());
    verify(blobApplicationAware, times(0)).localCleanup();
    assertThat(output.getOut(), containsString("Wrong name format:"));
  }

  @Test
  void shouldFilterMessageForFailedDownload() {

    // Mock the get step of the message handling
    doReturn(blobReceived).when(blobRestConnector).get(any(BlobApplicationAware.class));

    inputDestination.send(MessageBuilder.withPayload(myList).build());

    verify(blobRestConnector, times(1)).get(any());
    verify(blobProcessor, times(0)).process(any());
    verify(blobRestConnector, times(0)).deleteRemote(any());
    verify(blobApplicationAware, times(0)).localCleanup();
  }

  @Test
  void shouldFilterMessageForFailedProcess() {

    // Mock the get step of the message handling
    doReturn(blobDownloaded).when(blobRestConnector).get(any(BlobApplicationAware.class));
    doReturn(blobDownloaded).when(blobProcessor).process(any(BlobApplicationAware.class));

    inputDestination.send(MessageBuilder.withPayload(myList).build());

    verify(blobRestConnector, times(1)).get(any());
    verify(blobProcessor, times(1)).process(any());
    verify(blobRestConnector, times(0)).deleteRemote(any());
    verify(blobApplicationAware, times(0)).localCleanup();
  }

  @Test
  void shouldFilterMessageForFailedRemoteDelete() {

    // Mock the get step of the message handling
    doReturn(blobDownloaded).when(blobRestConnector).get(any(BlobApplicationAware.class));
    doReturn(blobProcessed).when(blobProcessor).process(any(BlobApplicationAware.class));
    doReturn(blobProcessed).when(blobRestConnector).deleteRemote(any(BlobApplicationAware.class));

    inputDestination.send(MessageBuilder.withPayload(myList).build());

    verify(blobRestConnector, times(1)).get(any());
    verify(blobProcessor, times(1)).process(any());
    verify(blobRestConnector, times(1)).deleteRemote(any());
    verify(blobApplicationAware, times(0)).localCleanup();
  }

}
