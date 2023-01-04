package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("enrolled_payment_instrument")
@AllArgsConstructor
@Data
public class PaymentInstrumentItem {

  @Indexed(unique = true)
  @Field(name = "hashPan")
  private String hashPan;

}
