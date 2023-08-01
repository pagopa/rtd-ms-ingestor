package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Thi calss represents the mapping of a blob storage to Java object.
 */
@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class BlobApplicationAware {

  /**
   * File lifecycle statuses.
   */
  public enum Status {
    INIT, RECEIVED, DOWNLOADED, PROCESSED, REMOTELY_DELETED, LOCALLY_DELETED,
  }

  private String blobUri;
  private String container;
  private String blob;
  private Status status;
  private String targetContainer;

  private String targetContainerRtd = "rtd-transactions-decrypted";

  private String targetDir = "/tmp";

  private Pattern uriPattern =
      Pattern.compile("^.*containers/((rtd)-transactions-decrypted)/blobs/(.*)");

  private static final String WRONG_FORMAT_NAME_WARNING_MSG = "Wrong name format:";
  private static final String EVENT_NOT_OF_INTEREST_WARNING_MSG = "Event not of interest:";

  private static final String FAIL_FILE_DELETE_WARNING_MSG = "Failed to delete local file:";

  /**
   * Constructor.
   *
   * @param uri the blob URI
   */
  public BlobApplicationAware(String uri) {
    blobUri = uri;
    status = Status.INIT;

    Matcher matcher = uriPattern.matcher(uri);

    if (matcher.matches()) {
      container = matcher.group(1);
      blob = matcher.group(3);

      // Tokenized blob name for checking compliance
      String[] blobNameTokenized = blob.split("\\.");

      if (checkNameFormat(blobNameTokenized)) {
        targetContainer = targetContainerRtd;
        status = Status.RECEIVED;
      } else {
        log.warn(WRONG_FORMAT_NAME_WARNING_MSG + blobUri);
      }
    } else {
      log.info(EVENT_NOT_OF_INTEREST_WARNING_MSG + blobUri);
    }
  }

  /**
   * This method matches PagoPA file name's standard Specifics can be found at:
   * https://docs.pagopa.it/digital-transaction-register/v/digital-transaction-filter/acquirer-integration-with-pagopa-centrostella/integration/standard-pagopa-file-transactions
   * ADE transactions are excluded here.
   *
   * @param uriTokens values obtained from the name of the blob (separated by dots)
   * @return true if the name matches the format, false otherwise
   */
  private boolean checkNameFormat(String[] uriTokens) {
    // Check if the tokens length is right
    if (uriTokens.length < 6) {
      return false;
    }

    // Check for application name (add new services to the regex)
    if (!uriTokens[0].matches("(CSTAR)")) {
      return false;
    }

    // Check for sender ABI code
    if (!uriTokens[1].matches("[a-zA-Z0-9]{5}")) {
      return false;
    }

    // Check for filetype (fixed "TRNLOG" value)
    // Should ignore case?
    if (!uriTokens[2].equalsIgnoreCase("TRNLOG")) {
      return false;
    }

    SimpleDateFormat daysFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    // Make the format refuse wrong date and time (default behavior is to overflow
    // values in
    // following date)
    daysFormat.setLenient(false);

    try {
      daysFormat.parse(uriTokens[3] + uriTokens[4]);
    } catch (ParseException e) {
      return false;
    }

    // Check for progressive value
    return uriTokens[5].matches("\\d{3}");
  }

  /**
   * This method deletes the local files left by the blob handling.
   */
  public BlobApplicationAware localCleanup() {
    boolean failCleanup = false;

    for (File f : Objects.requireNonNull(Path.of(this.targetDir).toFile().listFiles())) {
      // Delete every file in the temporary directory that starts with the name of the
      // blob.
      if (f.getName().startsWith(blob)) {
        try {
          Files.delete(f.toPath());
        } catch (Exception e) {
          log.warn(FAIL_FILE_DELETE_WARNING_MSG + f.getName() + " (" + e + ")");
          failCleanup = true;
        }
      }
    }

    if (!failCleanup) {
      status = Status.LOCALLY_DELETED;
    }
    return this;
  }
}
