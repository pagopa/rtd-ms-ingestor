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
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Thread safe HTTP client implementing pooling.
 */
@Configuration
public class ThreadSafeHttpClient {

  @Bean
  public CloseableHttpClient myHttpClient(WalletConfiguration configuration)
      throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    SSLContext sslContext =
        SSLContexts.custom().loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE).build();

    Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.INSTANCE).register("https",
            new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
        .build();

    PoolingHttpClientConnectionManager connectionManager =
        new PoolingHttpClientConnectionManager(registry);

    connectionManager.setMaxTotal(configuration.getConnectionPool());
    connectionManager.setDefaultMaxPerRoute(configuration.getConnectionPool());

    return HttpClients.custom()
        .setConnectionManager(connectionManager)
        .build();
  }
}
