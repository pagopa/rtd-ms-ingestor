package it.gov.pagopa.rtd.ms.rtdmsingestor.event;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware.Status;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.BlobRestConnector;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.EventProcessor;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

/**
 * Component defining the processing steps in response to storage events.
 */

// https://medium.com/codex/dealing-with-long-running-jobs-using-apache-kafka-192f053e1691
@Configuration
@Getter
public class EventHandler {

  private static final Logger log = LoggerFactory.getLogger(EventHandler.class);
  /**
   * Constructor.
   *
   * @return a consumer for Event Grid events.
   */

  @Bean
  public Consumer<Message<List<EventGridEvent>>> blobStorageConsumer(
      BlobRestConnector blobRestConnector, EventProcessor blobProcessor) {
    return message -> message.getPayload().stream()
            .filter(e -> "Microsoft.Storage.BlobCreated".equals(e.getEventType()))
            .map(EventGridEvent::getSubject).map(BlobApplicationAware::new)
            .filter(b -> Status.RECEIVED.equals(b.getStatus())).map(blobRestConnector::get)
            .filter(b -> Status.DOWNLOADED.equals(b.getStatus())).map(blobProcessor::process)
            .filter(b -> Status.PROCESSED.equals(b.getStatus())).map(blobRestConnector::deleteRemote)
            .filter(b -> Status.REMOTELY_DELETED.equals(b.getStatus()))
            .map(BlobApplicationAware::localCleanup)
            .filter(b -> Status.LOCALLY_DELETED.equals(b.getStatus()))
            .collect(Collectors.toList());
  }
}
