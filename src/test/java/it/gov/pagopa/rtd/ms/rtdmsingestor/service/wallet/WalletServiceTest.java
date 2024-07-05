package it.gov.pagopa.rtd.ms.rtdmsingestor.service.wallet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.WalletConfiguration;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.WalletContract;
import java.io.IOException;
import java.util.stream.IntStream;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WalletServiceTest {

  private CloseableHttpClient apacheClient;

  private WalletService walletService;

  @BeforeEach
  void setup() {
    apacheClient = Mockito.mock(CloseableHttpClient.class);
    walletService = new WalletService(new WalletConfiguration(
        "http://localhost:8080",
        "123",
        "/updateDetails",
        "/delete",
        10000,
        10000,
        10,
        10,
        3,
        10,
        25,
        25
        ),
        apacheClient
    );
  }

  @Test
  void shouldLimitWalletApiCalls() throws IOException {
    String serializedContract = "{ \"action\": \"CREATE\", \"import_outcome\": \"OK\", \"payment_method\": \"CARD\", \"method_attributes\": { \"pan_tail\": \"6295\", \"expdate\": \"04/28\", \"card_id_4\": \"6b4d345a594e69654478796546556c384c6955765a42794a345139305457424c394d794e4b4566466c44593d\", \"card_payment_circuit\": \"MC\", \"new_contract_identifier\": \"1e04de1f762b440fa5c444464603bc7c\", \"original_contract_identifier\": \"3b1288edc1f14e0a97129d84fbf1f01e\", \"card_bin\": \"459521\" } }";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonParser jsonParser = new JsonFactory().createParser(serializedContract);
    WalletContract contract = objectMapper.readValue(jsonParser, WalletContract.class);

    CloseableHttpResponse mockedResponse = Mockito.mock(CloseableHttpResponse.class);
    when(mockedResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1,
        HttpStatus.SC_OK, contract.getContractIdentifier()));
    doReturn(mockedResponse).when(apacheClient).execute(any(HttpPost.class));

    for (final var ignored : IntStream.range(0, 100).boxed().toList()) {
      walletService.postContract(contract.getMethodAttributes(), contract.getContractIdentifier());
    }

    verify(apacheClient, times(100)).execute(any());
  }

  @Test
  void shouldRetryWhenWalletReturns429() throws IOException {
    String serializedContract = "{ \"action\": \"CREATE\", \"import_outcome\": \"OK\", \"payment_method\": \"CARD\", \"method_attributes\": { \"pan_tail\": \"6295\", \"expdate\": \"04/28\", \"card_id_4\": \"6b4d345a594e69654478796546556c384c6955765a42794a345139305457424c394d794e4b4566466c44593d\", \"card_payment_circuit\": \"MC\", \"new_contract_identifier\": \"1e04de1f762b440fa5c444464603bc7c\", \"original_contract_identifier\": \"3b1288edc1f14e0a97129d84fbf1f01e\", \"card_bin\": \"459521\" } }";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonParser jsonParser = new JsonFactory().createParser(serializedContract);
    WalletContract contract = objectMapper.readValue(jsonParser, WalletContract.class);

    CloseableHttpResponse mockedResponse = Mockito.mock(CloseableHttpResponse.class);
    when(mockedResponse.getStatusLine()).thenReturn(
        new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_TOO_MANY_REQUESTS, contract.getContractIdentifier()),
        new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_TOO_MANY_REQUESTS, contract.getContractIdentifier()),
        new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, contract.getContractIdentifier())
    );
    doReturn(mockedResponse).when(apacheClient).execute(any(HttpPost.class));
    walletService.postContract(contract.getMethodAttributes(), contract.getContractIdentifier());
    verify(apacheClient, times(3)).execute(any(HttpPost.class));
  }

}
