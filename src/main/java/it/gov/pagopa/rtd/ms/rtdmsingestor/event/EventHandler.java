package it.gov.pagopa.rtd.ms.rtdmsingestor.event;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.EventGridEvent;

/**
 * Component defining the processing steps in response to storage events.
 */
@Configuration
@Getter
@Slf4j
public class EventHandler {

  /**
   * Constructor.
   *
   * @return a consumer for Event Grid events
   */
  @Bean
  public Consumer<Message<List<EventGridEvent>>> blobStorageConsumer() {
    return message -> message.getPayload().stream().
        map(m -> {
          log.error("{}", m.getSubject());
          return m;
        })
        .collect(Collectors.toList());
  }

}