package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import static it.gov.pagopa.rtd.ms.rtdmsingestor.model.FiscalCode.formatCf;
import static it.gov.pagopa.rtd.ms.rtdmsingestor.model.FiscalCode.validate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.rtd.ms.rtdmsingestor.model.FiscalCode.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class FiscalCodeTest {

  @Value("${ingestor.resources.base.path}")
  static String resources;

  @Test
  void shouldFormat() {
    assertEquals("PROVA", formatCf("P r o va"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      // Correct regular fiscal codes
      "RSSMRA80A01H501U", "RSS MRA 80 A0 1H501U", "KJWMFE88C50E205S", "GNNTIS14L02X498V",
      "JKNXZK26E16Y097M", "FOXLNI79S12C045Z", "CMRRNL59T02C064C",
      // Correct temporary fiscal codes
      "00000000000", "44444444440", "12345678903", "74700694370", "57636564049", "19258897628",
      "08882740981", "4730 9842  806"})
  void shouldBeCorrectFiscalCode(String cf) {
    assertEquals(Response.CORRECT_FISCAL_CODE, validate(cf));
  }

  @Test
  void shouldBeEmptyFiscalCode() {
    assertEquals(Response.EMPTY, validate(""));
  }

  // Skips the "RSSMRA80A01" case because, having 11 char, it's recognized as
  // Temporary Fiscal Code
  // Thus composed only by numbers.
  @ParameterizedTest
  @ValueSource(
      strings = {"R", "RS", "RSS", "RSSM", "RSSMR", "RSSMRA", "RSSMRA8", "RSSMRA80", "RSSMRA80A",
          "RSSMRA80A0", "RSSMRA80A01H", "RSSMRA80A01H5", "RSSMRA80A01H5L", "RSSMRA80A01H501"})
  void shouldHaveInvalidLengthFiscalCode(String cf) {
    assertEquals(Response.INVALID_LENGTH, validate(cf));
  }

  @Test
  void shouldHaveInvalidCharacteriscalCode() {
    assertEquals(Response.INVALID_CHARACTERS, validate("MRORSS00A+0A000V"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"RSSMRA80A01H50MU", "RSSMRA80A01H5LMU", "RSSMRA80A01HRLMU"})
  void shouldBeCorrectOmocodicFiscalCode(String cf) {
    assertEquals(Response.CORRECT_FISCAL_CODE, validate(cf));
  }

  @ParameterizedTest
  @ValueSource(strings = {"RSSMRA80A01H5L1U", "RSSMRA80A01HR01U", "RSSMRA80A01HRL1U"})
  void shouldFailOmocodicWrongPosition(String cf) {
    assertEquals(Response.INVALID_CHARACTERS, validate(cf));
  }

  @ParameterizedTest
  @ValueSource(strings = {"@@@@@@@@@@@@@@@@", "@@@@@@@@@@@", "RSSMRA80A0+1H501", "0000+000000"})
  void shouldFailInvalidCharacters(String cf) {
    assertEquals(Response.INVALID_CHARACTERS, validate(cf));
  }

  @ParameterizedTest
  @ValueSource(strings = {"RSSMRA80A01H501V", "12345678901", "00000000001"})
  void shouldFailInvalidChecksum(String cf) {
    assertEquals(Response.INVALID_CHECKSUM, validate(cf));
  }
}
