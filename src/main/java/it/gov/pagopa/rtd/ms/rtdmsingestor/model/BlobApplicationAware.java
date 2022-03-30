package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Thi calss represents the mapping of a blob storage to Java object.
 */
@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class BlobApplicationAware {

  String uri;

  public BlobApplicationAware(String uri) {
    this.uri = uri;
    log.info("Init: %s", uri);
  }


}
