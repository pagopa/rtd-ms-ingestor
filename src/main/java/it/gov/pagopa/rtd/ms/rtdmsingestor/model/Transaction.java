package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class represents the object containing transaction fields as attributes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"idTrxAcquirer", "acquirerCode", "trxDate"}, callSuper = false)
public class Transaction {

  String idTrxAcquirer;

  String acquirerCode;

  OffsetDateTime trxDate;

  String hpan;

  String operationType;

  String circuitType;

  String idTrxIssuer;

  String correlationId;

  BigDecimal amount;

  String amountCurrency;

  String mcc;

  String acquirerId;

  String merchantId;

  String terminalId;

  String bin;

}
