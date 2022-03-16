package it.gov.pagopa.rtd.ms.rtdmsingestor.model;


import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
