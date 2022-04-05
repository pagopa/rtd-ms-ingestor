package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import com.opencsv.bean.CsvBindByPosition;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
public class Transaction {

  @CsvBindByPosition(position = 0)
  String acquirerCode;

  @CsvBindByPosition(position = 1)
  String operationType;

  @CsvBindByPosition(position = 2)
  String circuitType;

  @CsvBindByPosition(position = 3)
  String hpan;

  @CsvBindByPosition(position = 4)
  String trxDate;

  @CsvBindByPosition(position = 5)
  String idTrxAcquirer;

  @CsvBindByPosition(position = 7)
  String idTrxIssuer;

  @CsvBindByPosition(position = 8)
  String correlationId;

  @CsvBindByPosition(position = 9)
  BigDecimal totalAmount;

  @CsvBindByPosition(position = 10)
  String amountCurrency;

  @CsvBindByPosition(position = 11)
  String acquirerId;

  @CsvBindByPosition(position = 12)
  String merchantId;

  @CsvBindByPosition(position = 13)
  String terminalId;

  @CsvBindByPosition(position = 14)
  String bin;

  @CsvBindByPosition(position = 15)
  String mcc;

  @CsvBindByPosition(position = 16)
  String fiscalCode;

  @CsvBindByPosition(position = 17)
  String vat;

  @CsvBindByPosition(position = 18)
  String posType;

  @CsvBindByPosition(position = 19)
  String par;
}
