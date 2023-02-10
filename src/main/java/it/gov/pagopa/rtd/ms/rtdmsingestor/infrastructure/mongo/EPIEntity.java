package it.gov.pagopa.rtd.ms.rtdmsingestor.infrastructure.mongo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

@Document("enrolled_payment_instrument")
@Builder(toBuilder = true)
public class EPIEntity {

  @Id
  private String id;

  @Indexed(unique = true)
  @Field(name = "hashPan")
  private String hashPan;

  @Indexed
  @Field("hashPanChildren")
  private List<String> hashPanChildren;

  @Indexed
  @Field(name = "enabledApps")
  private List<String> apps;

  @Field(name = "par")
  private String par;

  @Field(name = "state")
  private String state;

  @Field(name = "issuer")
  private String issuer;

  @Field(name = "network")
  private String network;

  @Field(name = "insertAt")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime insertAt;

  @Field(name = "updatedAt")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime updatedAt;

  @Field(name = "insertUser")
  private String insertUser;

  @Field(name = "updateUser")
  private String updateUser;

  @Version
  private int version;

  @Field(name = "hashPanExports")
  private List<String> hashPanExports;

}
