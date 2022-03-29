package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Thi calss represents the mapping of a blob storage to Java object.
 */
@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class BlobApplicationAware {

  String uri;

  public BlobApplicationAware init(String uri) {
    this.uri = uri;
    log.info("Init: %s", uri);
    return this;
  }


}
