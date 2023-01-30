package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Properties of an event published to an Event Grid topic.
 * https://docs.microsoft.com/en-us/azure/event-grid/event-schema
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DeadLetterQueueEvent {

  /**
  * The transaction data.
  */
  @JsonProperty(value = "transaction", required = true)
  private Transaction transaction;

  @JsonProperty(value = "mongo_exception", required = true)
  private String mongoException;

}
