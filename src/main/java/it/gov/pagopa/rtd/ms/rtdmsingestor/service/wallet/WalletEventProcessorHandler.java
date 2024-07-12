package it.gov.pagopa.rtd.ms.rtdmsingestor.service.wallet;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import it.gov.pagopa.rtd.ms.rtdmsingestor.adapter.ContractAdapter;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.properties.WalletConfigurationProperties;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsingestor.model.WalletContract;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.EventProcessorHandler;
import it.gov.pagopa.rtd.ms.rtdmsingestor.utils.Anonymizer;
import it.gov.pagopa.rtd.ms.rtdmsingestor.utils.OpenTelemetryKeys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class WalletEventProcessorHandler implements EventProcessorHandler {

    private static final String CREATE_ACTION = "CREATE";
    private static final String DELETE_ACTION = "DELETE";

    private final WalletService walletService;
    private final ContractAdapter adapter;
    private final Anonymizer anonymizer;
    private final Integer threadPoolSize;
    private final WalletConfigurationProperties configuration;

    public WalletEventProcessorHandler(
            WalletService walletService,
            ContractAdapter adapter,
            Anonymizer anonymizer,
            WalletConfigurationProperties configuration) {
        this.walletService = walletService;
        this.adapter = adapter;
        this.anonymizer = anonymizer;
        this.configuration = configuration;
        this.threadPoolSize = configuration.getThreadPool();
    }

    @Override
    public BlobApplicationAware processEvent(BlobApplicationAware blob) {
        final Path blobPath = Path.of(blob.getTargetDir(), blob.getBlob());
        return processWalletContracts(blob, blobPath);
    }

    private BlobApplicationAware processWalletContracts(
            BlobApplicationAware blob,
            Path blobPath
    ) {
        log.info("Extracting contracts from:{}", blob.getBlobUri());

        int numFailedContracts = 0;
        int numTotalContracts = 0;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();

        List<Callable<Boolean>> walletImportTaskList = new ArrayList<>();

        try (InputStream inputStream = new FileInputStream(blobPath.toFile())) {
            JsonParser jsonParser = jsonFactory.createParser(inputStream);

            if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
                log.error("Validation error: expected wallet export contracts array");
                return blob;
            }

            while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                WalletContract contract = objectMapper.readValue(
                        jsonParser,
                        WalletContract.class
                );
                int currNumTotalContracts = ++numTotalContracts;

                walletImportTaskList.add(() ->
                        processContractWrapper(
                                blob.getBlob(),
                                contract,
                                currNumTotalContracts
                        )
                );
            }
        } catch (JsonParseException | MismatchedInputException e) {
            log.error(
                    "Validation error: malformed wallet export at position [{}]",
                    numTotalContracts
            );
        } catch (IOException e) {
            log.error("Missing blob file:{}", blobPath);
        }

        // Local Execution Service
        ExecutorService executorService = Executors.newFixedThreadPool(
                threadPoolSize
        );

        // InvokeAll
        try {
            List<Future<Boolean>> walletImportTaskResult = executorService.invokeAll(
                    walletImportTaskList
            );
            for (Future<Boolean> task : walletImportTaskResult) {
                if (!task.get()) {
                    numFailedContracts++;
                }
            }
        } catch (InterruptedException e) {
            log.error(
                    "Unexpected thread interruption error during result collection",
                    e
            );
        } catch (Exception e) {
            log.error("Error processing contract", e);
            numFailedContracts++;
        }

        if (numFailedContracts == 0) {
            log.info("Blob {} processed successfully", blob.getBlob());
        } else {
            log.info(
                    "Blob {} processed with {} failures",
                    blob.getBlob(),
                    numFailedContracts
            );
        }

        executorService.shutdown();

        // The file is still considered processed in order to delete decrypted export file
        blob.setStatus(BlobApplicationAware.Status.PROCESSED);
        return blob;
    }

    @WithSpan
    private boolean processContractWrapper(
            String fileName,
            WalletContract contract,
            int contractPosition
    ) {
        boolean updateOutcome = processContract(contract, contractPosition);
        this.logImportOutcome(fileName, contract, contractPosition, updateOutcome);
        return updateOutcome;
    }

    private boolean processContract(
            WalletContract contract,
            int contractFilePosition
    ) {
        if (contract.getAction() == null) {
            log.error("Null action on contract at {}", contractFilePosition);
            return false;
        }

        contract = adapter.adapt(contract);
        String currContractId;
        String contractIdHmac;

        if (!"OK".equals(contract.getImportOutcome())) {
            currContractId = contract.getContractIdentifier();
            contractIdHmac = anonymizer.anonymize(currContractId);
            MDC.put(OpenTelemetryKeys.Wallet.MDC_CONTRACT_ID, contractIdHmac);
            return true;
        }

        if (contract.getAction().equals(CREATE_ACTION)) {
            currContractId = contract.getMethodAttributes().getContractIdentifier();
            contractIdHmac = anonymizer.anonymize(currContractId);
            MDC.put(OpenTelemetryKeys.Wallet.MDC_CONTRACT_ID, contractIdHmac);
            if (
                    !walletService.postContract(contract.getMethodAttributes(), contractIdHmac)
            ) {
                log.error(
                        "Failed saving contract at position {}",
                        contractFilePosition
                );
                return false;
            } else {
                return true;
            }
        } else if (contract.getAction().equals(DELETE_ACTION)) {
            currContractId = contract.getContractIdentifier();
            contractIdHmac = anonymizer.anonymize(currContractId);
            MDC.put(OpenTelemetryKeys.Wallet.MDC_CONTRACT_ID, contractIdHmac);
            if (
                    !walletService.deleteContract(
                            contract.getContractIdentifier(),
                            contractIdHmac
                    )
            ) {
                log.error(
                        "Failed deleting contract at position {}",
                        contractFilePosition
                );
                return false;
            } else {
                return true;
            }
        }
        log.error(
                "Unrecognized action on contract at position {}",
                contractFilePosition
        );
        return false;
    }

    private void logImportOutcome(
            String fileName,
            WalletContract contract,
            int contractPosition,
            boolean importOutcome
    ) {
        MDC.put("Filename", fileName);
        MDC.put("Position", String.valueOf(contractPosition));
        MDC.put("Action", contract.getAction());
        MDC.put("ImportOutcome", contract.getImportOutcome());
        MDC.put("Successful", String.valueOf(importOutcome));
        log.info("");
        MDC.clear();
    }
}
