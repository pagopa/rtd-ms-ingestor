package it.gov.pagopa.rtd.ms.rtdmsingestor.service;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.exceptions.CsvConstraintViolationException;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.Transaction;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public class TransactionVerifier implements BeanVerifier<Transaction> {

  private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

  private static final Validator validator = factory.getValidator();

  @Override
  public boolean verifyBean(Transaction transaction)
      throws CsvConstraintViolationException {
    Set<ConstraintViolation<Transaction>> violations = validator.validate(
        transaction);

    if (!violations.isEmpty()) {
      StringBuilder malformedFields = new StringBuilder();
      for (ConstraintViolation<Transaction> violation : violations) {
        malformedFields
            .append("(")
            .append(violation.getPropertyPath().toString())
            .append(": ");
        malformedFields.append(violation.getMessage()).append(") ");
      }

      throw new CsvConstraintViolationException(
          "Malformed fields extracted: {}" + malformedFields);
    }

    return true;
  }
}
