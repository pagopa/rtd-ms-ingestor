package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo;

import lombok.Builder;
import lombok.Data;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("enrolled_payment_instrument")
@Builder(toBuilder = true)
@Data
public class EPIItem {

  @Indexed(unique = true)
  @Field(name = "hashPan")
  private String hashPan;

}
