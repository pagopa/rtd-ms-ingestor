package it.gov.pagopa.rtd.ms.rtdmsingestor.configuration;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Thread safe HTTP client implementing pooling.
 */
@Configuration
@Slf4j
public class ThreadSafeHttpClient {

  @Bean
  CloseableHttpClient myHttpClient()
      throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    SSLContext sslContext =
        SSLContexts.custom().loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE).build();

    Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.INSTANCE).register("https",
            new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
        .build();

    PoolingHttpClientConnectionManager connectionManager =
        new PoolingHttpClientConnectionManager(registry);

    connectionManager.setMaxTotal(25);
    connectionManager.setDefaultMaxPerRoute(25);

    return HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
        .addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
          log.info(
              "Before - Leased Connections = " + connectionManager.getTotalStats().getLeased());
          log.info("Before - Available Connections = " + connectionManager.getTotalStats()
              .getAvailable());
        })
        .addInterceptorFirst((HttpResponseInterceptor) (response, context) -> {
          log.info("After - Leased Connections = " + connectionManager.getTotalStats().getLeased());
          log.info("After - Available Connections = " + connectionManager.getTotalStats()
              .getAvailable());
        })
        .build();
  }
}
