package it.gov.pagopa.rtd.ms.rtdmsingestor.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@Slf4j
public class Anonymizer {

  private static final String FAILED_HMAC_STUB = "###FAILED_TO_PRODUCE_HMAC###";
  private static final String ALGORITHM = "HmacSHA256";


  private final String secretKey;

    public Anonymizer(
            @Value("${ingestor.contractIdObfuscationHmacKey}") String secretKey
    ) {
        this.secretKey = secretKey;
    }

    /**
   * This method anonymize a given string with a determined algorithm and a key recovered from
   *
   * @param toAnonymize string to anonymize
   * @return the anonymized string
   */
  public String anonymize(String toAnonymize) {
    try {
      byte[] bytes = Base64.getDecoder().decode(secretKey);
      SecretKeySpec secretKeySpec = new SecretKeySpec(bytes, ALGORITHM);
      Mac hmac = Mac.getInstance(ALGORITHM);
      hmac.init(secretKeySpec);
      byte[] hmacBytes = hmac.doFinal(toAnonymize.getBytes());
      return Hex.encodeHexString(hmacBytes);
    } catch (NullPointerException e) {
      log.error("Empty contract identifier");
      return FAILED_HMAC_STUB;
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      log.error("Cannot produce HMAC: {}", e.getMessage());
      return FAILED_HMAC_STUB;
    }
  }

}
