package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo;

import java.util.List;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Builder;

@Document("enrolled_payment_instrument")
@Builder(toBuilder = true)
public class EPIPair {

  @Indexed(unique = true)
  @Field(name = "hashPan")
  private String hashPan;

  @Indexed
  @Field("hashPanChildren")
  private List<String> hashPanChildren;

}
