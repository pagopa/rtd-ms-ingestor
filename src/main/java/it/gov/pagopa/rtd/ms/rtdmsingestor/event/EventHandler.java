package it.gov.pagopa.rtd.ms.rtdmsingestor.event;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.BlobRestConnector;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

/**
 * Component defining the processing steps in response to storage events.
 */
@Configuration
@Getter
public class EventHandler {

  @Autowired
  BlobRestConnector blobRestConnector;

  /**
   * Constructor.
   *
   * @return a consumer for Event Grid events.
   */
  @Bean
  public Consumer<Message<List<EventGridEvent>>> blobStorageConsumer() {
    return message -> message.getPayload().stream()
        .filter(e -> "Microsoft.Storage.BlobCreated".equals(e.getEventType()))
        .map(EventGridEvent::getSubject).map(BlobApplicationAware::new)
        .map(blobRestConnector::download).map(blobRestConnector::open)
        .map(blobRestConnector::produce).collect(Collectors.toList());
  }

}
