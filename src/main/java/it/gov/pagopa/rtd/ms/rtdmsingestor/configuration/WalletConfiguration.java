package it.gov.pagopa.rtd.ms.rtdmsingestor.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ingestor.api.wallet")
@Getter
@Setter
@AllArgsConstructor
public class WalletConfiguration {
  private String baseUrl;
  private String apiKey;
  private String updateContracts;
  private String deleteContracts;
  private Integer readTimeout;
  private Integer connectionTimeout;
  private Integer maxRetryAttempt;
  private Integer retryMaxIntervalMilliSeconds;
  private Integer threadPool;
  private Integer connectionPoolPerRoute;
  private Integer maxConnectionPool;
  private Integer defaultHttpKeepAlive;
}
