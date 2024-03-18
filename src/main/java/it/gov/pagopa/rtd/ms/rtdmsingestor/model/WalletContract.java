package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class WalletContract {

  @JsonAlias("action")
  @Pattern(regexp = "CREATE|DELETE", flags = Pattern.Flag.CASE_INSENSITIVE)
  @NotNull
  private String action;

  @JsonAlias("import_outcome")
  @NotNull
  @Pattern(regexp = "OK|KO", flags = Pattern.Flag.CASE_INSENSITIVE)
  private String importOutcome;

  @JsonAlias("payment_method")
  @JsonInclude(Include.NON_NULL)
  private String paymentMethod;

  @JsonAlias("method_attributes")
  @JsonInclude(Include.NON_NULL)
  private ContractMethodAttributes methodAttributes;

  @JsonAlias("reason_message")
  @JsonInclude(Include.NON_NULL)
  private String reasonMessage;

  @JsonAlias("original_contract_identifier")
  @JsonInclude(Include.NON_NULL)
  private String originalContractIdentifier;
}
