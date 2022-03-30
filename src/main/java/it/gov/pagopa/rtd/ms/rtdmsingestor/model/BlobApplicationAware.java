package it.gov.pagopa.rtd.ms.rtdmsingestor.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    INIT,
    RECEIVED,
    DOWNLOADED,
  }

  private String blobUri;
  private String container;
  private String blob;
  private Status status;
  private String targetContainer;

  private String targetContainerRtd = "rtd-transactions-decrypted";

  private String targetDir = "/tmp";

  private Pattern uriPattern = Pattern.compile(
      "^.*containers/((rtd)-transactions-[a-z0-9]{44})/blobs/(.*)");

  private static final String WRONG_FORMAT_NAME_WARNING_MSG = "Wrong name format:";
  private static final String CONFLICTING_SERVICE_WARNING_MSG = "Conflicting service in URI:";
  private static final String EVENT_NOT_OF_INTEREST_WARNING_MSG = "Event not of interest:";

  private static final String FAIL_FILE_DELETE_WARNING_MSG = "Failed to delete local blob file:";

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

      //Tokenized blob name for checking compliance
      String[] blobNameTokenized = blob.split("\\.");

      if (checkNameFormat(blobNameTokenized)) {

        //Check whether the blob's service matches in path and name,
        // then assign the target container
        if (matcher.group(2).equalsIgnoreCase("RTD")
            && blobNameTokenized[0].equalsIgnoreCase("CSTAR")) {
          targetContainer = targetContainerRtd;
          status = Status.RECEIVED;
        } else {
          log.warn(CONFLICTING_SERVICE_WARNING_MSG + blobUri);
        }
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
    // Check for application name (add new services to the regex)
    if (uriTokens[0] == null || !uriTokens[0].matches("(CSTAR)")) {
      return false;
    }

    // Check for sender ABI code
    if (uriTokens[1] == null || !uriTokens[1].matches("[a-zA-Z0-9]{5}")) {
      return false;
    }

    // Check for filetype (fixed "TRNLOG" value)
    // Should ignore case?
    if (uriTokens[2] == null || !uriTokens[2].equalsIgnoreCase("TRNLOG")) {
      return false;
    }

    // Check for creation timestamp correctness
    if (uriTokens[3] == null || uriTokens[4] == null) {
      return false;
    }

    SimpleDateFormat daysFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    // Make the format refuse wrong date and time (default behavior is to overflow values in
    // following date)
    daysFormat.setLenient(false);

    try {
      daysFormat.parse(uriTokens[3] + uriTokens[4]);
    } catch (ParseException e) {
      return false;
    }

    // Check for progressive value
    return (uriTokens[5] != null) && uriTokens[5].matches("[0-9]{3}");
  }

}
