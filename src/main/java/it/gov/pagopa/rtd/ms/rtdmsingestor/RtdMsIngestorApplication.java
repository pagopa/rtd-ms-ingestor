package it.gov.pagopa.rtd.ms.rtdmsingestor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RTD microsrvice to ingest on a Kafka Topic decrypted files contsinint
 * electronic payments.
 */
@SpringBootApplication
public class RtdMsIngestorApplication {
  /**
   * Main method to run the Ingestor Miscroservice.
   *
   * @param args arguments.
   */
  public static void main(String[] args) {
    SpringApplication.run(RtdMsIngestorApplication.class, args);
  }
}
