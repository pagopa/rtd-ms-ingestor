package it.gov.pagopa.rtd.ms.rtdmsingestor.configs;

import de.flapdoodle.embed.mongo.config.MongoCmdOptions;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

import java.io.IOException;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@AutoConfigureBefore(EmbeddedMongoAutoConfiguration.class)
public class MongoDbTestConfig {

  private static final String IP = "localhost";
  private static final int PORT = 28017;

  @Bean
  public MongodConfig embeddedMongoConfiguration() throws IOException {
    return MongodConfig.builder()
        .version(Version.Main.PRODUCTION)
        .net(new Net(IP, PORT, Network.localhostIsIPv6()))
        .cmdOptions(MongoCmdOptions.builder().useNoJournal(false).build())
        .build();
  }
}
