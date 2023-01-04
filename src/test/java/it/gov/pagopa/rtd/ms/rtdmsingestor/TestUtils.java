package it.gov.pagopa.rtd.ms.rtdmsingestor;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestUtils {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random random = new Random();

  public static String generateRandomHashPan() {
    return IntStream.range(0, 64)
            .mapToObj(i -> "" + ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())))
            .collect(Collectors.joining(""));
  }


}
