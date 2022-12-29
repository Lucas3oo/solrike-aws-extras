/*
 * Copyright © 2022 Lucas Persson. All Rights Reserved.
 */
package se.solrike.awsrdsiamdatasourcefactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import se.solrike.awsrdsiamdatasourcefactory.sample.GenericRdsIamDatasourceFactory;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

/**
 *
 */
class GenericRdsIamDatasourceFactoryTest {

  /*
   * This test will not try to connect to a real DB nor connect to the AWS RDS service so
   * it will be kind of limited.
   */
  @Test
  void testCreateDatasource() throws SQLException {
    // given datasource factory and datasource properties for MySql setup
    RdsUtilities rdsUtilities = mock(RdsUtilities.class);
    String token = "randomToken";
    when(rdsUtilities.generateAuthenticationToken(any(GenerateAuthenticationTokenRequest.class))).thenReturn(token);
    GenericRdsIamDatasourceFactory factory = new GenericRdsIamDatasourceFactory(rdsUtilities);

    // when datasource is created
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:mysql://localhost:3306/simpsons");
    config.setUsername("bart");
    config.setPassword("51mp50n");
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("iam-authentication-token-refresh-interval-in-minutes", 14);
    HikariDataSource dataSource = factory.dataSource(config);

    // then the password shall be the token
    assertThat(dataSource.getPassword()).isEqualTo(token);
    // and then the call to RDS shall contain username, host and port from the config
    GenerateAuthenticationTokenRequest expectedRequest = GenerateAuthenticationTokenRequest.builder()
        .hostname("localhost")
        .port(3306)
        .username("bart")
        .build();

    verify(rdsUtilities)
        .generateAuthenticationToken(argThat(new GenerateAuthenticationTokenRequestMatcher(expectedRequest)));

  }

}

/*
 * GenerateAuthenticationTokenRequest doesn't implement equals so we need to make our own
 */
class GenerateAuthenticationTokenRequestMatcher implements ArgumentMatcher<GenerateAuthenticationTokenRequest> {

  private final GenerateAuthenticationTokenRequest mExpected;

  public GenerateAuthenticationTokenRequestMatcher(GenerateAuthenticationTokenRequest expected) {
    mExpected = expected;
  }

  @Override
  public boolean matches(GenerateAuthenticationTokenRequest actual) {
    return mExpected.hostname().equals(actual.hostname()) && mExpected.username().equals(actual.username());
  }

}
