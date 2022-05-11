package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import static it.gov.pagopa.rtd.ms.rtdmsingestor.model.FiscalCode.Response.INVALID_CHARACTERS;
import static it.gov.pagopa.rtd.ms.rtdmsingestor.model.FiscalCode.Response.INVALID_CHECKSUM;
import static it.gov.pagopa.rtd.ms.rtdmsingestor.model.FiscalCode.Response.INVALID_LENGTH;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.FiscalCode.Response;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

/**
 * This is the implementation of a custom validator. This validator checks the correctness of an
 * italian fiscal code.
 */
public class FiscalCodeValidator implements
    ConstraintValidator<FiscalCodeConstraint, String> {

  @Override
  public boolean isValid(String codiceFiscale,
      ConstraintValidatorContext context) {

    //If codiceFiscale is null, then the validation is passed because it is not a mandatory field
    if (codiceFiscale == null || codiceFiscale.equals("")) {
      return true;
    }

    HibernateConstraintValidatorContext hibernateContext =
        context.unwrap(HibernateConstraintValidatorContext.class);
    hibernateContext.disableDefaultConstraintViolation();

    Response checkedCodFis = FiscalCode.validate(codiceFiscale);

    if (checkedCodFis.equals(INVALID_CHARACTERS)) {
      hibernateContext.buildConstraintViolationWithTemplate(
              "Invalid character for Fiscal Code " + codiceFiscale)
          .addConstraintViolation();
      return false;
    }
    if (checkedCodFis.equals(INVALID_LENGTH)) {
      hibernateContext.buildConstraintViolationWithTemplate(
              "Invalid length for Fiscal Code " + codiceFiscale)
          .addConstraintViolation();
      return false;
    }
    if (checkedCodFis.equals(INVALID_CHECKSUM)) {
      hibernateContext.buildConstraintViolationWithTemplate(
              "Invalid checksum for Fiscal Code " + codiceFiscale)
          .addConstraintViolation();
      return false;
    }
    return true;
  }

}