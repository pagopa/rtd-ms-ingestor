package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractMethodAttributes {

  @JsonProperty("pan_tail")
  @Pattern(regexp = "\\d{4}")
  private String panTail;

  @JsonProperty("expdate")
  @NotNull
  private String expdate;

  @JsonProperty("card_id_4")
  @NotNull
  private String cardId4;

  @JsonProperty("card_payment_circuit")
  @NotNull
  private String cardPaymentCircuit;

  @JsonProperty("new_contract_identifier")
  @NotNull
  private String newContractIdentifier;

  @JsonProperty("original_contract_identifier")
  @NotNull
  private String originalContractIdentifier;

  @JsonProperty("card_bin")
  @NotNull
  @Pattern(regexp = "\\d{6}")
  private String cardBin;
}
