package it.gov.pagopa.rtd.ms.rtdmsingestor;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import it.gov.pagopa.rtd.ms.rtdmsingestor.event.EventHandler;
import it.gov.pagopa.rtd.ms.rtdmsingestor.event.EventHandlerIntegration;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.BlobRestConnector;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EmbeddedKafka(topics = {"rtd-platform-events", "rtd-trx"}, partitions = 1,
    bootstrapServersProperty = "spring.embedded.kafka.brokers")
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class})
@ContextConfiguration(classes = {EventHandler.class, EventHandlerIntegration.class})
@TestPropertySource(value = {"classpath:application-test.yml"}, inheritProperties = false)
@DirtiesContext
class RtdMsIngestorApplicationTests {

  @Autowired
  private StreamBridge stream;

  @SpyBean
  private BlobApplicationAware blobApplicationAware;

  @SpyBean
  private BlobRestConnector blobRestConnector;

  @MockBean
  CloseableHttpClient client;

  private final String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
  private final String blob = "CSTAR.99910.TRNLOG.20220316.103107.001.csv.pgp";
  private final String blobUri = "/blobServices/default/containers/" + container + "/blobs/" + blob;

  private final String myId = "my_id";
  private final String myTopic = "my_topic";
  private final String myEventType = "Microsoft.Storage.BlobCreated";

  @Test
  void shouldSendMessageOnRtdQueue() {

    BlobApplicationAware blobDownloaded = new BlobApplicationAware(blobUri);
    blobDownloaded.setStatus(BlobApplicationAware.Status.DOWNLOADED);
    doReturn(blobDownloaded).when(blobRestConnector).download(any(BlobApplicationAware.class));

    EventGridEvent myEvent = new EventGridEvent();
    myEvent.setId(myId);
    myEvent.setTopic(myTopic);
    myEvent.setEventType(myEventType);
    myEvent.setSubject(blobUri);
    List<EventGridEvent> myList = new ArrayList<EventGridEvent>();
    myList.add(myEvent);

    assertThat("Should Send",
        stream.send("blobStorageConsumer-in-0", MessageBuilder.withPayload(myList).build()));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      verify(blobRestConnector, times(1)).download(any());

    });
  }


  @Test
  public void shouldReceiveMessgeFromRtdQueue() {
    Transaction t = new Transaction();
    t.setIdTrxAcquirer("idtrx");
    stream.send("rtdTrxProducer-out-0", MessageBuilder.withPayload(t).build());

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

      verify(blobRestConnector, atLeastOnce())
          .test(ArgumentMatchers.<Message<Transaction>>any());

    });
  }

}
