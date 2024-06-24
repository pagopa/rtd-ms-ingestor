package it.gov.pagopa.rtd.ms.rtdmsingestor.service.wallet;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.WalletContract;

public record WalletImportTaskData(
    WalletContract contract,
    int contractFilePosition
) {}
