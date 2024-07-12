package it.gov.pagopa.rtd.ms.rtdmsingestor.service.wallet;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.WalletConfiguration;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.WalletContract;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletServiceTest {

  private static final int MAX_RETRY = 3;

  private CloseableHttpClient apacheClient;
  private WalletService walletService;

  @BeforeEach
  void setup() {
    apacheClient = Mockito.mock(CloseableHttpClient.class);
    walletService =
      new WalletService(
        new WalletConfiguration(
          "http://localhost:8080",
          "123",
          "/updateDetails",
          "/delete",
          10000,
          10000,
          MAX_RETRY,
          1000,
          10000,
          3,
          10,
          25
        ),
        apacheClient
      );
  }

  @Test
  void shouldRetryWhenWalletReturns429() throws IOException {
    WalletContract contract = generateContract();
    WalletService.ParsedHttpResponse tooManyRequestResponse = new WalletService.ParsedHttpResponse(429, "", "");
    WalletService.ParsedHttpResponse successResponse = new WalletService.ParsedHttpResponse(200, "", "");
    when(apacheClient.execute(any(HttpPost.class), any(ResponseHandler.class)))
            .thenReturn(
                    tooManyRequestResponse,
                    tooManyRequestResponse,
                    successResponse
            );
    assertTrue(walletService.postContract(contract.getMethodAttributes(), contract.getContractIdentifier()));
    verify(apacheClient, times(3)).execute(any(HttpPost.class), any(ResponseHandler.class));
  }

  @Test
  void shouldRetryWhenWalletReturns5xx() throws IOException {
    WalletContract contract = generateContract();
    WalletService.ParsedHttpResponse mockedResponse = new WalletService.ParsedHttpResponse(500, "", "");
    when(apacheClient.execute(any(HttpPost.class), any(ResponseHandler.class))).thenReturn(mockedResponse);
    assertFalse(
            walletService.postContract(contract.getMethodAttributes(), contract.getContractIdentifier()));
    verify(apacheClient, times(MAX_RETRY)).execute(any(HttpPost.class), any(ResponseHandler.class));
  }

  private WalletContract generateContract() throws IOException {
    String serializedContract =
            "{ \"action\": \"CREATE\", \"import_outcome\": \"OK\", \"payment_method\": \"CARD\", \"method_attributes\": { \"pan_tail\": \"6295\", \"expdate\": \"04/28\", \"card_id_4\": \"6b4d345a594e69654478796546556c384c6955765a42794a345139305457424c394d794e4b4566466c44593d\", \"card_payment_circuit\": \"MC\", \"new_contract_identifier\": \"1e04de1f762b440fa5c444464603bc7c\", \"original_contract_identifier\": \"3b1288edc1f14e0a97129d84fbf1f01e\", \"card_bin\": \"459521\" } }";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonParser jsonParser = new JsonFactory().createParser(serializedContract);
    return objectMapper.readValue(
            jsonParser,
            WalletContract.class
    );
  }
}
