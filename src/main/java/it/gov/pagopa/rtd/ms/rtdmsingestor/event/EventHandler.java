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

  @Autowired
  private BindingsLifecycleController bindingsLifecycleController;

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  private static final String BINDER_NAME = "blobStorageConsumer-in-0";

  @Bean
  public Consumer<Message<List<EventGridEvent>>> blobStorageConsumer(
      BlobRestConnector blobRestConnector, EventProcessor blobProcessor) {
    return message -> {
      pauseConsumer(BINDER_NAME);
      executorService.execute(() -> {
        try {
          message.getPayload().stream()
                  .filter(e -> "Microsoft.Storage.BlobCreated".equals(e.getEventType()))
                  .map(EventGridEvent::getSubject).map(BlobApplicationAware::new)
                  .filter(b -> Status.RECEIVED.equals(b.getStatus())).map(blobRestConnector::get)
                  .filter(b -> Status.DOWNLOADED.equals(b.getStatus())).map(blobProcessor::process)
                  .filter(b -> Status.PROCESSED.equals(b.getStatus())).map(blobRestConnector::deleteRemote)
                  .filter(b -> Status.REMOTELY_DELETED.equals(b.getStatus()))
                  .map(BlobApplicationAware::localCleanup)
                  .filter(b -> Status.LOCALLY_DELETED.equals(b.getStatus()))
                  .collect(Collectors.toList());
        } catch (Exception e) {
          log.error("Error while processing event", e);
        } finally {
          resumeConsumer(BINDER_NAME);
          log.info("Consumer status [{}]", getConsumerState(BINDER_NAME));
        }
      });
      log.info("Consumer status [{}]", getConsumerState(BINDER_NAME));
    };
  }

  private void pauseConsumer(String bindingName) {
    bindingsLifecycleController.changeState(bindingName, BindingsLifecycleController.State.PAUSED);
  }

  private void resumeConsumer(String bindingName) {
    bindingsLifecycleController.changeState(bindingName, BindingsLifecycleController.State.RESUMED);
  }

  private String getConsumerState(String bindingName) {
    return bindingsLifecycleController.queryState(bindingName).stream().findFirst()
            .map(binding -> binding.isPaused() ? "PAUSED" : binding.isRunning() ? "RUNNING" : "UNKNOWN")
            .orElse("UNKNOWN");
  }
}
