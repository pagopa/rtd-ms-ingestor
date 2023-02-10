package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import static it.gov.pagopa.rtd.ms.rtdmsingestor.model.FiscalCode.Response.INVALID_CHARACTERS;
import static it.gov.pagopa.rtd.ms.rtdmsingestor.model.FiscalCode.Response.INVALID_CHECKSUM;
import static it.gov.pagopa.rtd.ms.rtdmsingestor.model.FiscalCode.Response.INVALID_LENGTH;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.FiscalCode.Response;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

/**
 * This is the implementation of a custom validator. This validator checks the correctness of an
 * italian fiscal code.
 */
@Slf4j
public class FiscalCodeValidator implements ConstraintValidator<FiscalCodeConstraint, String> {

  @Override
  public boolean isValid(String codiceFiscale, ConstraintValidatorContext context) {
    // If codiceFiscale is null, then the validation is passed because it is not a
    // mandatory field
    if (codiceFiscale == null || codiceFiscale.equals("")) {
      log.warn("Empty Fiscal Code");
    }

    Response checkedCodFis = FiscalCode.validate(codiceFiscale);

    if (checkedCodFis.equals(INVALID_CHARACTERS)) {
      log.error("Invalid character for Fiscal Code " + codiceFiscale);
    }
    if (checkedCodFis.equals(INVALID_LENGTH)) {
      log.error("Invalid length for Fiscal Code " + codiceFiscale);
    }
    if (checkedCodFis.equals(INVALID_CHECKSUM)) {
      log.error("Invalid checksum for Fiscal Code " + codiceFiscale);
    }
    return true;
  }
}
