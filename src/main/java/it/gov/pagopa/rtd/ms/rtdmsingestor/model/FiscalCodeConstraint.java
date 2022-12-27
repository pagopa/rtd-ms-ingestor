package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Interface for validate fiscal codes (in italian format).
 */
@Documented
@Constraint(validatedBy = FiscalCodeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FiscalCodeConstraint {

  /**
   * Message in case of violated constraint.
   *
   * @return a message explaining the violation.
   */
  String message() default "Invalid fiscal code.";

  /**
   * Groups parameter.
   */
  Class<?>[] groups() default {};

  /**
   * Payload.
   */
  Class<? extends Payload>[] payload() default {};
}
