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
@Constraint(validatedBy = CodiceFiscaleValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CodiceFiscaleConstraint {

  /**
   * Message in case of violated constraint.
   *
   * @return a message explaining the violation.
   */
  String message() default "Invalid fiscal code.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}