package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.ThreadSafeHttpClient;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.WalletConfiguration;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;

class HttpClientTest {

  @SpyBean
  WalletConfiguration configuration = new WalletConfiguration(
      "http://localhost:8080",
      "123",
      "/updateDetails",
      "/delete",
      5,
      3,
      25,
      25
  );

  @Test
  void shouldNotBeClosed()
      throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    CloseableHttpClient client = new ThreadSafeHttpClient().myHttpClient(configuration);
    String uri = "https://eu.httpbin.org/get";
    final HttpGet getBlob = new HttpGet(uri);
    CloseableHttpResponse response;
    response = client.execute(getBlob);
    assertEquals(200, response.getStatusLine().getStatusCode());
  }

}
