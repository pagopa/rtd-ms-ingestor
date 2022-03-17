package it.gov.pagopa.rtd.ms.rtdmsingestor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import it.gov.pagopa.rtd.ms.rtdmsingestor.event.EventHandler;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(topics = {"rtd-platform-events"}, partitions = 1,
		bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers")
class RtdMsIngestorApplicationTests {

	@SpyBean
	EventHandler eh;

	@Autowired
	private StreamBridge stream;

	@Autowired
	private Supplier<Message<Transaction>> producer;

	@Test
	void shouldConsumeMessage() {

		String container = "rtd-transactions-32489876908u74bh781e2db57k098c5ad034341i8u7y";
		String blob = "CSTAR.99910.TRNLOG.20220228.103107.001.csv.pgp";

		EventGridEvent my_event = new EventGridEvent();
		my_event.setId("my_id");
		my_event.setTopic("my_topic");
		my_event.setEventType("Microsoft.Storage.BlobCreated");
		my_event.setSubject("/blobServices/default/containers/" + container + "/blobs/" + blob);
		List<EventGridEvent> my_list = new ArrayList<EventGridEvent>();
		my_list.add(my_event);

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {

			stream.send("blobStorageConsumer-in-0", MessageBuilder.withPayload(my_list).build());

			verify(eh, times(1)).blobStorageConsumer();

		});

	}

	@Test
	void shouldProduceMessage() {

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			stream.send("rtdTrxProducer-out-0", producer.get());
			verify(eh, times(1)).rtdTrxProducer();

		});

	}

}
