package it.gov.pagopa.rtd.ms.rtdmsingestor.event;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.BlobRestConnector;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

/**
 * Component defining the processing steps in response to storage events.
 */
@Configuration
@Getter
@Slf4j
public class EventHandlerIntegration {

  @Autowired
  BlobRestConnector blobRestConnector;

  /**
   * Event handler triggered by the insertion of a transaction on the input queue.
   *
   * @return a consumer handling events.
   */
  //@Bean
  public Consumer<Message<Transaction>> rtdTrxConsumer() {
    return message -> {
      //log.info("Received transaction:" + message.getPayload());
    };
  }
}
