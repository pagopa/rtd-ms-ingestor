package it.gov.pagopa.rtd.ms.rtdmsingestor.configuration;

import it.gov.pagopa.rtd.ms.rtdmsingestor.adapter.ContractAdapter;
import it.gov.pagopa.rtd.ms.rtdmsingestor.configuration.properties.WalletConfigurationProperties;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.wallet.WalletEventProcessorHandler;
import it.gov.pagopa.rtd.ms.rtdmsingestor.service.wallet.WalletService;
import it.gov.pagopa.rtd.ms.rtdmsingestor.utils.Anonymizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WalletConfiguration {

    @Bean
    WalletEventProcessorHandler walletEventProcessorHandler(
            WalletConfigurationProperties walletConfigurationProperties,
            WalletService walletService,
            Anonymizer anonymizer
    ) {
        return new WalletEventProcessorHandler(
                walletService,
                new ContractAdapter(),
                anonymizer,
                walletConfigurationProperties.getThreadPool()
        );
    }

}
