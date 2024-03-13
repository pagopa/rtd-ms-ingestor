package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
public class WalletContract {

  @JsonProperty("action")
  @Pattern(regexp = "CREATE|DELETE", flags = Pattern.Flag.CASE_INSENSITIVE)
  @NotNull
  private String action;

  @JsonProperty("import_outcome")
  @NotNull
  @Pattern(regexp = "OK|KO", flags = Pattern.Flag.CASE_INSENSITIVE)
  private String importOutcome;

  @JsonProperty("payment_method")
  @JsonInclude(Include.NON_NULL)
  private String paymentMethod;

  @JsonProperty("method_attributes")
  @JsonInclude(Include.NON_NULL)
  private ContractMethodAttributes methodAttributes;

  @JsonProperty("reason_message")
  @JsonInclude(Include.NON_NULL)
  private String reasonMessage;

  @JsonProperty("original_contract_identifier")
  @JsonInclude(Include.NON_NULL)
  private String originalContractIdentifier;
}
