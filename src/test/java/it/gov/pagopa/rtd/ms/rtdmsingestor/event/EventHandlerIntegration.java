package it.gov.pagopa.rtd.ms.rtdmsingestor.event;

import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.BlobApplicationAware;

/**
 * Component defining the processing steps in response to storage events.
 */
@Configuration
@Getter
@Slf4j
public class EventHandlerIntegration {

  @Bean
  public Consumer<Message<Transaction>> rtdTrxConsumer(
      BlobApplicationAware blobApplicationAware) {
    return message -> {
      blobApplicationAware.test(message);
      log.info("\n\n\n\n\n\nTEST\n\n\n\n\n\n");
    };
  }

}
