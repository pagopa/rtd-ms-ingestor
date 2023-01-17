package it.gov.pagopa.rtd.ms.rtdmsingestor.model;


/**
 * Italian Codice Fiscale normalization, formatting and validation routines. A <u>regular CF</u> is
 * composed by 16 among letters and digits; the last character is always a letter representing the
 * control code. A <u>temporary CF</u> could also be assigned; a temporary CF is composed of 11
 * digits, the last digit being the control code. Examples: MRORSS00A00A000U, 12345678903.
 *
 * @author Umberto Salsi salsi@icosaedro.it.
 * @version 2020-01-24
 */
public class FiscalCode {

  /**
   * Enumeration of possible validation responses.
   */
  public enum Response {
    CORRECT_FISCAL_CODE,
    INVALID_CHARACTERS,
    INVALID_CHECKSUM,
    INVALID_LENGTH,
    EMPTY,
  }

  /**
   * Normalizes a CF by removing white spaces and converting to upper-case. Useful to clean-up
   * user's input and to save the result in the DB.
   *
   * @param cf Raw CF, possibly with spaces.
   * @return Normalized CF.
   */
  static String normalize(String cf) {
    cf = cf.replaceAll("[ \t\r\n]", "");
    cf = cf.toUpperCase();
    return cf;
  }

  /**
   * Returns the formatted CF. Currently does nothing but normalization.
   *
   * @param cf Raw CF, possibly with spaces.
   * @return Formatted CF.
   */
  static String formatCf(String cf) {
    return normalize(cf);
  }

  /**
   * Validates a regular CF.
   *
   * @param cf Normalized, 16 characters CF.
   * @return Null if valid, or string describing why this CF must be rejected.
   */
  private static Response validateRegular(String cf) {

    //This array stores the presence of substitution codes in the last 3 digits in case of homocody.
    // This routine doesn't allow homocodic codes further than 3 digits!
    boolean[] homocode = new boolean[3];
    if (!cf.matches(
        "^[A-Z]{6}\\d{2}[ABCDEHLMPRST]\\d{2}[0-9A-Z]{4}[A-Z]$")) {
      return Response.INVALID_CHARACTERS;
    }

    int s = 0;
    String evenMap = "BAFHJNPRTVCESULDGIMOQKWZYX";
    for (int i = 0; i < 15; i++) {
      int c = cf.charAt(i);
      int n;

      if ((i >= 12) && String.valueOf(cf.charAt(i)).matches("[LMN]")) {
        c = cf.charAt(i) - 'L' + '0';
        homocode[i - 12] = true;
      }

      //The substitution characters skips the O
      if ((i >= 12) && String.valueOf(cf.charAt(i)).matches("[PQRSTUV]")) {
        c = cf.charAt(i) - 'L' - 1 + '0';
        homocode[i - 12] = true;
      }

      if ('0' <= c && c <= '9') {
        n = c - '0';
      } else {
        n = c - 'A';
      }
      if ((i & 1) == 0) {
        n = evenMap.charAt(n) - 'A';
      }
      s += n;
    }

    if (s % 26 + 'A' != cf.charAt(15)) {
      return Response.INVALID_CHECKSUM;
    }

    return validHomocode(homocode);
  }

  /**
   * Validates a temporary CF.
   *
   * @param cf Normalized, 11 characters CF.
   * @return Null if valid, or string describing why this CF must be rejected.
   */
  private static Response validateTemporary(String cf) {
    if (!cf.matches("^\\d{11}$")) {
      return Response.INVALID_CHARACTERS;
    }
    int s = 0;
    for (int i = 0; i < 11; i++) {
      int n = cf.charAt(i) - '0';
      if ((i & 1) == 1) {
        n *= 2;
        if (n > 9) {
          n -= 9;
        }
      }
      s += n;
    }
    if (s % 10 != 0) {
      return Response.INVALID_CHECKSUM;
    }
    return Response.CORRECT_FISCAL_CODE;
  }

  /**
   * Verifies the basic syntax, length and control code of the given CF.
   *
   * @param cf Raw CF, possibly with spaces.
   * @return Null if valid, or string describing why this CF must be rejected.
   */
  static Response validate(String cf) {
    cf = normalize(cf);
    if (cf.length() == 0) {
      return Response.EMPTY;
    } else if (cf.length() == 16) {
      return validateRegular(cf);
    } else if (cf.length() == 11) {
      return validateTemporary(cf);
    } else {
      return Response.INVALID_LENGTH;
    }
  }

  private static Response validHomocode(boolean[] homocode) {
    //Check for the order of the omocodic substitution characters
    if (homocode[0]) {
      if (homocode[1]) {
        if (!homocode[2]) {
          return Response.INVALID_CHARACTERS;
        }
      } else {
        return Response.INVALID_CHARACTERS;
      }
    }

    if (homocode[1] && !homocode[2]) {
      return Response.INVALID_CHARACTERS;
    }
    return Response.CORRECT_FISCAL_CODE;
  }
}
