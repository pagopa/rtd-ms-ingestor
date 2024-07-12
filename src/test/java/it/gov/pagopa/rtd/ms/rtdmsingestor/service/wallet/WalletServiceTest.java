package it.gov.pagopa.rtd.ms.rtdmsingestor.service.wallet;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.properties.WalletConfigurationProperties;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.WalletContract;
import it.gov.pagopa.rtd.ms.rtdmsingestor.utils.Anonymizer;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletServiceTest {

  private static final WalletConfigurationProperties stubConfigurationProperties = new WalletConfigurationProperties(
          "http://localhost:8080",
          "123",
          "/updateDetails",
          "/delete",
          10000,
          10000,
          10,
          1000,
          10000,
          3,
          10,
          25,
          25,
          25,
          5
  );

  private final CloseableHttpClient apacheClient = Mockito.mock(CloseableHttpClient.class);
  private final Anonymizer anonymizer = new Anonymizer("123");

  private final WalletService walletService = new WalletService(stubConfigurationProperties, apacheClient);

  @BeforeEach
  void setup() {
    reset(apacheClient);
  }

  @Test
  void shouldRetryWhenWalletReturns429() throws IOException {
    String serializedContract =
      "{ \"action\": \"CREATE\", \"import_outcome\": \"OK\", \"payment_method\": \"CARD\", \"method_attributes\": { \"pan_tail\": \"6295\", \"expdate\": \"04/28\", \"card_id_4\": \"6b4d345a594e69654478796546556c384c6955765a42794a345139305457424c394d794e4b4566466c44593d\", \"card_payment_circuit\": \"MC\", \"new_contract_identifier\": \"1e04de1f762b440fa5c444464603bc7c\", \"original_contract_identifier\": \"3b1288edc1f14e0a97129d84fbf1f01e\", \"card_bin\": \"459521\" } }";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonParser jsonParser = new JsonFactory().createParser(serializedContract);
    WalletContract contract = objectMapper.readValue(
      jsonParser,
      WalletContract.class
    );

    CloseableHttpResponse mockedResponse = Mockito.mock(
      CloseableHttpResponse.class
    );
    when(mockedResponse.getStatusLine())
      .thenReturn(
        new BasicStatusLine(
          HttpVersion.HTTP_1_1,
          HttpStatus.SC_TOO_MANY_REQUESTS,
          contract.getContractIdentifier()
        ),
        new BasicStatusLine(
          HttpVersion.HTTP_1_1,
          HttpStatus.SC_TOO_MANY_REQUESTS,
          contract.getContractIdentifier()
        ),
        new BasicStatusLine(
          HttpVersion.HTTP_1_1,
          HttpStatus.SC_OK,
          contract.getContractIdentifier()
        )
      );
    doReturn(mockedResponse).when(apacheClient).execute(any(HttpPost.class), any(ResponseHandler.class));
    walletService.postContract(
      contract.getMethodAttributes(),
      contract.getContractIdentifier()
    );
    verify(apacheClient, times(3)).execute(any(HttpPost.class), any(ResponseHandler.class));
  }

  @Test
  void shouldDeleteContract() throws IOException {
    String serializedContract = "{ \"action\": \"DELETE\", \"import_outcome\": \"OK\", \"original_contract_identifier\": \"3b1288edc1f14e0a97129d84fbf1f01e\" }";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonParser jsonParser = new JsonFactory().createJsonParser(serializedContract);
    WalletContract contract = objectMapper.readValue(jsonParser, WalletContract.class);
    final var mockedResponse = new WalletService.ParsedHttpResponse(204, "", "");
    doReturn(mockedResponse).when(apacheClient).execute(any(HttpPost.class), any(ResponseHandler.class));
    String currContractId = contract.getContractIdentifier();
    String contractIdHmac = anonymizer.anonymize(currContractId);

    assertTrue(walletService.deleteContract(contract.getContractIdentifier(), contractIdHmac));
    verify(apacheClient, times(1)).execute(any(HttpPost.class), any(ResponseHandler.class));
  }

  @Test
  void shouldNotUpdateContractBadResponse() throws IOException {

    String serializedContract = "{ \"action\": \"CREATE\", \"import_outcome\": \"OK\", \"payment_method\": \"CARD\", \"method_attributes\": { \"pan_tail\": \"6295\", \"expdate\": \"04/28\", \"card_id_4\": \"6b4d345a594e69654478796546556c384c6955765a42794a345139305457424c394d794e4b4566466c44593d\", \"card_payment_circuit\": \"MC\", \"new_contract_identifier\": \"1e04de1f762b440fa5c444464603bc7c\", \"original_contract_identifier\": \"3b1288edc1f14e0a97129d84fbf1f01e\", \"card_bin\": \"459521\" } }";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonParser jsonParser = new JsonFactory().createJsonParser(serializedContract);
    WalletContract contract = objectMapper.readValue(jsonParser, WalletContract.class);

    final var mockedResponse = new WalletService.ParsedHttpResponse(HttpStatus.SC_TOO_MANY_REQUESTS, "Too many requests", "Too many requests");

    doReturn(mockedResponse).when(apacheClient).execute(any(HttpPost.class), any(ResponseHandler.class));
    String currContractId = contract.getMethodAttributes().getContractIdentifier();
    String contractIdHmac = anonymizer.anonymize(currContractId);

    assertFalse(walletService.postContract(contract.getMethodAttributes(), contractIdHmac));
    verify(apacheClient, times(3)).execute(any(HttpPost.class), any(ResponseHandler.class));
  }

  @Test
  void shouldNotUpdateContractExceptionDuringExecute() throws IOException {
    String serializedContract = "{ \"action\": \"CREATE\", \"import_outcome\": \"OK\", \"payment_method\": \"CARD\", \"method_attributes\": { \"pan_tail\": \"6295\", \"expdate\": \"04/28\", \"card_id_4\": \"6b4d345a594e69654478796546556c384c6955765a42794a345139305457424c394d794e4b4566466c44593d\", \"card_payment_circuit\": \"MC\", \"new_contract_identifier\": \"1e04de1f762b440fa5c444464603bc7c\", \"original_contract_identifier\": \"3b1288edc1f14e0a97129d84fbf1f01e\", \"card_bin\": \"459521\" } }";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonParser jsonParser = new JsonFactory().createJsonParser(serializedContract);
    WalletContract contract = objectMapper.readValue(jsonParser, WalletContract.class);

    doThrow(new IOException()).when(apacheClient).execute(any(HttpPost.class), any(ResponseHandler.class));
    String currContractId = contract.getMethodAttributes().getContractIdentifier();
    String contractIdHmac = anonymizer.anonymize(currContractId);

    assertFalse(walletService.postContract(contract.getMethodAttributes(), contractIdHmac));
    verify(apacheClient, times(3)).execute(any(HttpPost.class), any(ResponseHandler.class));
  }

  @Test
  void shouldNotDeleteContractBadResponse() throws IOException {
    String serializedContract = "{ \"action\": \"DELETE\", \"import_outcome\": \"OK\", \"original_contract_identifier\": \"3b1288edc1f14e0a97129d84fbf1f01e\" }";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonParser jsonParser = new JsonFactory().createJsonParser(serializedContract);
    WalletContract contract = objectMapper.readValue(jsonParser, WalletContract.class);

    final var mockedResponse = new WalletService.ParsedHttpResponse(HttpStatus.SC_NOT_FOUND, "Contract not found", "Contract not found");
    doReturn(mockedResponse).when(apacheClient).execute(any(HttpPost.class), any(ResponseHandler.class));
    String currContractId = contract.getContractIdentifier();
    String contractIdHmac = anonymizer.anonymize(currContractId);

    assertFalse(walletService.deleteContract(contract.getContractIdentifier(), contractIdHmac));
    verify(apacheClient, times(1)).execute(any(HttpPost.class), any(ResponseHandler.class));
  }

  @Test
  void shouldNotDeleteContractExceptionDuringExecute() throws IOException {
    String serializedContract = "{ \"action\": \"DELETE\", \"import_outcome\": \"OK\", \"original_contract_identifier\": \"3b1288edc1f14e0a97129d84fbf1f01e\" }";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonParser jsonParser = new JsonFactory().createJsonParser(serializedContract);
    WalletContract contract = objectMapper.readValue(jsonParser, WalletContract.class);

    doThrow(new IOException()).when(apacheClient).execute(any(HttpPost.class), any(ResponseHandler.class));
    String currContractId = contract.getContractIdentifier();
    String contractIdHmac = anonymizer.anonymize(currContractId);

    assertFalse(walletService.deleteContract(contract.getContractIdentifier(), contractIdHmac));
    verify(apacheClient, times(3)).execute(any(HttpPost.class), any(ResponseHandler.class));
  }

}
