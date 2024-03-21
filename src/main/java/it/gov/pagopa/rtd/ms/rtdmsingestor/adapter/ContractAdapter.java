package it.gov.pagopa.rtd.ms.rtdmsingestor.adapter;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.ContractMethodAttributes;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.WalletContract;
import org.springframework.stereotype.Service;

@Service
public class ContractAdapter {

  public WalletContract adapt(WalletContract contract) {
    ContractMethodAttributes methodAttributes = contract.getMethodAttributes();
    if (methodAttributes != null && (methodAttributes.getCardPaymentCircuit().equals("MC"))) {
      methodAttributes.setCardPaymentCircuit("MASTERCARD");
      contract.setMethodAttributes(methodAttributes);
    }
    return contract;
  }

}
