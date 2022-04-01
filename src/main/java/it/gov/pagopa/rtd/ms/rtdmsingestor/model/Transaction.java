package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.sf.jsefa.csv.annotation.CsvDataType;
import net.sf.jsefa.csv.annotation.CsvField;

/**
 * This class represents the object containing transaction fields as attributes.
 * The format is based on the one specified at:
 * https://docs.pagopa.it/digital-transaction-register/v/digital-transaction-filter/acquirer-integration-with-pagopa-centrostella/integration/standard-pagopa-file-transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"idTrxAcquirer", "acquirerCode", "trxDate"}, callSuper = false)
@CsvDataType()
public class Transaction {

  @CsvField(pos = 1)
  String acquirerCode;

  @CsvField(pos = 2)
  String operationType;

  @CsvField(pos = 3)
  String circuitType;

  @CsvField(pos = 4)
  String hpan;

  //TODO sistema il formato
  @CsvField(pos = 5)
  String trxDate;

  @CsvField(pos = 6)
  String idTrxAcquirer;

  @CsvField(pos = 7)
  String idTrxIssuer;

  @CsvField(pos = 8)
  String correlationId;

  @CsvField(pos = 9)
  BigDecimal totalAmount;

  @CsvField(pos = 10)
  String amountCurrency;

  @CsvField(pos = 11)
  String acquirerId;

  @CsvField(pos = 12)
  String merchantId;

  @CsvField(pos = 13)
  String terminalId;

  @CsvField(pos = 14)
  String bin;

  @CsvField(pos = 15)
  String mcc;

  @CsvField(pos = 16)
  String fiscalCode;

  @CsvField(pos = 17)
  String vat;

  @CsvField(pos = 18)
  String posType;

  @CsvField(pos = 19)
  String par;
}
