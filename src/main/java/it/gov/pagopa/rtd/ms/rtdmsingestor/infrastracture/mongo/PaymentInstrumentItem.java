package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastracture.mongo;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("enrolled_payment_instrument")
@Data
public class PaymentInstrumentItem {

  @Indexed(unique = true)
  @Field(name = "hashPan")
  private String hashPan;

}
