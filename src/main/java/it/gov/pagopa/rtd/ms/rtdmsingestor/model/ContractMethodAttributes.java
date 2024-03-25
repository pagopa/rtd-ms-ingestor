package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import com.fasterxml.jackson.annotation.JsonAlias;
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

  @JsonAlias("pan_tail")
  @Pattern(regexp = "\\d{4}")
  private String panTail;

  @JsonAlias("expdate")
  @NotNull
  private String expdate;

  @JsonAlias("card_id_4")
  @NotNull
  private String cardId4;

  @JsonAlias("card_payment_circuit")
  @NotNull
  private String cardPaymentCircuit;

  @JsonAlias("new_contract_identifier")
  @NotNull
  private String newContractIdentifier;

  @JsonAlias("original_contract_identifier")
  @NotNull
  private String originalContractIdentifier;

  @JsonAlias("card_bin")
  @NotNull
  @Pattern(regexp = "\\d{6}|\\d{8}")
  private String cardBin;
}
