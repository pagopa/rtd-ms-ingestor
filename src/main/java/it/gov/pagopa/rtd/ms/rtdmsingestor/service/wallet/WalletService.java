package it.gov.pagopa.rtd.ms.rtdmsingestor.service.wallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.WalletConfiguration;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.ContractMethodAttributes;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WalletService {

  private final CloseableHttpClient httpClient;
  private final Retry retry;
  private final RateLimiter rateLimiter;

  private final String walletBaseUrl;
  private final String updateContractsEndpoint;
  private final String deleteContractsEndpoint;
  private final String walletApiKey;

  private static final String APIM_SUBSCRIPTION_HEADER = "Ocp-Apim-Subscription-Key";
  private static final String CONTRACT_HMAC_HEADER = "x-contract-hmac";

  public WalletService(
      WalletConfiguration configuration,
      CloseableHttpClient httpClient
  ) {
    this.retry = Retry.of("wallet-retry", RetryConfig
        .<HttpResponse>custom()
        .retryOnResult(
            result -> result.getStatusLine().getStatusCode() == HttpStatus.SC_TOO_MANY_REQUESTS ||
                result.getStatusLine().getStatusCode() >= HttpStatus.SC_INTERNAL_SERVER_ERROR)
        .maxAttempts(configuration.getMaxRetryAttempt())
        .intervalFunction(IntervalFunction.ofRandomized(Duration.ofSeconds(configuration.getRateLimitTimeoutSeconds())))
        .failAfterMaxAttempts(true)
        .build()
    );
    this.rateLimiter = RateLimiter.of("wallet-ratelimit", RateLimiterConfig.custom()
        .limitForPeriod(configuration.getRateLimit())
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .timeoutDuration(Duration.ofSeconds(10))
        .build());

    this.walletBaseUrl = configuration.getBaseUrl();
    this.walletApiKey = configuration.getApiKey();
    this.updateContractsEndpoint = configuration.getUpdateContracts();
    this.deleteContractsEndpoint = configuration.getDeleteContracts();
    this.httpClient = httpClient;
  }

  public boolean postContract(ContractMethodAttributes contract, String contractHmac) {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String contractJson;

    try {
      contractJson = ow.writeValueAsString(contract);
    } catch (JsonProcessingException e) {
      log.error("Cannot serialize contract");
      return false;
    }

    StringEntity contractEntity = new StringEntity(
        contractJson,
        ContentType.APPLICATION_JSON);

    String uri = walletBaseUrl + updateContractsEndpoint;
    final HttpPost postContract = new HttpPost(uri);
    postContract.setEntity(contractEntity);
    postContract.setHeader(new BasicHeader(APIM_SUBSCRIPTION_HEADER, walletApiKey));
    postContract.setHeader(new BasicHeader(CONTRACT_HMAC_HEADER, contractHmac));

    final CheckedSupplier<CloseableHttpResponse> request = Retry.decorateCheckedSupplier(
        retry,
        RateLimiter.decorateCheckedSupplier(
            rateLimiter,
            () -> httpClient.execute(postContract)
        )
    );

    try (CloseableHttpResponse myResponse = request.get()) {
      int statusCode = myResponse.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        log.debug("Successfully updated contract");
        return true;
      } else {
        log.error("Can't update contract. Invalid HTTP response: {}, {}", statusCode,
            myResponse.getStatusLine().getReasonPhrase());
        return false;
      }
    } catch (Throwable ex) {
      log.error("Can't update contract. Unexpected error: {}", ex.getMessage());
      return false;
    }
  }

  public boolean deleteContract(String contractIdentifier, String contractHmac) {
    String uri = walletBaseUrl + deleteContractsEndpoint;
    final HttpPost deleteContract = new HttpPost(uri);
    deleteContract.setHeader(new BasicHeader(APIM_SUBSCRIPTION_HEADER, walletApiKey));
    deleteContract.setHeader(new BasicHeader(CONTRACT_HMAC_HEADER, contractHmac));

    StringEntity newContractIdentifierEntity = new StringEntity(
        "{\"contractIdentifier\": \"" + contractIdentifier + "\"}",
        ContentType.APPLICATION_JSON);
    deleteContract.setEntity(newContractIdentifierEntity);

    final CheckedSupplier<CloseableHttpResponse> request = Retry.decorateCheckedSupplier(
        retry,
        RateLimiter.decorateCheckedSupplier(
            rateLimiter,
            () -> httpClient.execute(deleteContract)
        )
    );

    try (CloseableHttpResponse myResponse = request.get()) {
      int statusCode = myResponse.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_NO_CONTENT) {
        log.debug("Successfully updated contract");
        return true;
      } else {
        log.error("Can't delete contract. Invalid HTTP response: {}, {}", statusCode,
            myResponse.getStatusLine().getReasonPhrase());
        return false;
      }
    } catch (Throwable ex) {
      log.error("Can't delete contract. Unexpected error: {}", ex.getMessage());
      return false;
    }
  }
}
