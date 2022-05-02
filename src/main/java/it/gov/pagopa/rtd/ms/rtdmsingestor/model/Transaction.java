package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvDate;
import java.math.BigDecimal;
import java.util.Date;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class represents the object containing transaction fields as attributes. The format is based
 * on the one specified at: https://docs.pagopa.it/digital-transaction-register/v/digital-transaction-filter/acquirer-integration-with-pagopa-centrostella/integration/standard-pagopa-file-transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"idTrxAcquirer", "acquirerCode", "trxDate"}, callSuper = false)
public class Transaction {

  @CsvBindByPosition(position = 0)
  @Pattern(regexp = "[a-zA-Z0-9]{5}", message = "ABI length must be 5 alphanumeric char")
  String acquirerCode;

  @CsvBindByPosition(position = 1)
  @Pattern(regexp = "[0-9]{2}", message = "Operation type length must match [0-9]{2}")
  String operationType;

  @CsvBindByPosition(position = 2)
  @Pattern(regexp = "[0-9]{2}", message = "Circuit type length must match [0-9]{2}")
  String circuitType;

  @CsvBindByPosition(position = 3)
  @Pattern(regexp = "[a-zA-Z0-9]{1,64}",
      message = "HPAN length must be 64 alphanumeric alphanumeric char at max")
  String hpan;

  //ISO8601
  @NotNull
  @CsvBindByPosition(position = 4)
  @CsvDate(value = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
  Date trxDate;

  @CsvBindByPosition(position = 5)
  @Pattern(regexp = "[a-zA-Z0-9]{1,255}",
      message = "ID trx acquirer length must be 255 alphanumeric char at max")
  String idTrxAcquirer;

  @CsvBindByPosition(position = 6)
  @Pattern(regexp = "[a-zA-Z0-9]{1,255}",
      message = "ID trx issuer length must be 255 alphanumeric char at max")
  String idTrxIssuer;

  @CsvBindByPosition(position = 7)
  @Pattern(regexp = "[a-zA-Z0-9]{0,255}",
      message = "Correlation ID length must be 255 alphanumeric char at max")
  String correlationId;

  @NotNull
  @CsvBindByPosition(position = 8)
  BigDecimal amount;

  @CsvBindByPosition(position = 9)
  @Pattern(regexp = "978",
      message = "Currency must be 978 (fixed value, ISO 4217 for Euro)")
  String amountCurrency;

  @CsvBindByPosition(position = 10)
  @Pattern(regexp = "[a-zA-Z0-9]{1,255}",
      message = "Acquirer ID length must be 255 alphanumeric char at max")
  String acquirerId;

  @CsvBindByPosition(position = 11)
  @Pattern(regexp = "[a-zA-Z0-9]{1,255}",
      message = "Merchant ID length must be 255 alphanumeric char at max")
  String merchantId;

  @CsvBindByPosition(position = 12)
  @Pattern(regexp = "[a-zA-Z0-9]{1,255}",
      message = "Terminal ID length must be 255 alphanumeric char at max")
  String terminalId;

  @CsvBindByPosition(position = 13)
  @Pattern(regexp = "[0-9]{6}|[0-9]{8}",
      message = "Correlation ID length must match [0-9]{6}|[0-9]{8}")
  String bin;

  @CsvBindByPosition(position = 14)
  @Pattern(regexp = "[a-zA-Z0-9]{1,5}", message = "MCC length must be 5 alphanumeric char at max")
  String mcc;

  @CsvBindByPosition(position = 15)
  @Pattern(regexp = "[a-zA-Z0-9]{1,50}",
      message = "Fiscal code length must be 5 alphanumeric char at max")
  String fiscalCode;

  @CsvBindByPosition(position = 16)
  @Pattern(regexp = "[a-zA-Z0-9]{0,50}",
      message = "VAT code length must be 50 alphanumeric char at max")
  String vat;

  @CsvBindByPosition(position = 17)
  @Pattern(regexp = "[0-9]{2}", message = "Pos type must match [0-9]{2}")
  String posType;

  @CsvBindByPosition(position = 18)
  String par;
}
