package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.ThreadSafeHttpClient;
import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = {ThreadSafeHttpClient.class})
@TestPropertySource(value = {"classpath:application-test.yml"}, inheritProperties = false)
class HttpClientTest {

  @Autowired
  CloseableHttpClient myClient;

  @Test
  void shouldNotBeClosed() throws IOException {
    String uri = "https://eu.httpbin.org/get";
    final HttpGet getBlob = new HttpGet(uri);
    CloseableHttpResponse response;
    response = myClient.execute(getBlob);
    assertEquals(200, response.getStatusLine().getStatusCode());
  }

}
