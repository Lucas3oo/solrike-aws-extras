/*
 * Copyright © 2022 Lucas Persson. All Rights Reserved.
 */
package se.solrike.aws.extras.datasourcefactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.awaitility.Durations;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;

import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

/**
 *
 */
class RdsIamAuthenticationTokenRefresherTest {

  @Test
  void testGetToken() {
    // given RDS client exists
    RdsUtilities rdsUtilities = mock(RdsUtilities.class);
    when(rdsUtilities.generateAuthenticationToken(any(GenerateAuthenticationTokenRequest.class)))
        .thenReturn("randomToken");

    // and given a token refresher
    RdsIamAuthenticationTokenRefresher refresher = new RdsIamAuthenticationTokenRefresher(rdsUtilities,
        "mydb.example.com", 3360, "myDbUsername");

    // when get token is called
    String token = refresher.getAuthenticationToken();

    // then a token shall be generated
    assertThat(token).isNotBlank();
    // and the RDS client shall have been invoked
    verify(rdsUtilities).generateAuthenticationToken(any(GenerateAuthenticationTokenRequest.class));
  }

  @Test
  void testStart() {
    // given RDS client and HikariConfigMXBean exists
    RdsUtilities rdsUtilities = mock(RdsUtilities.class);
    HikariConfig hikariConfig = new HikariConfig();
    String token = "randomToken";
    when(rdsUtilities.generateAuthenticationToken(any(GenerateAuthenticationTokenRequest.class))).thenReturn(token);

    // when creating the token refresher
    RdsIamAuthenticationTokenRefresher refresher = new RdsIamAuthenticationTokenRefresher(rdsUtilities,
        "mydb.example.com", 3360, "myDbUsername");

    // when starting the token refresher with interval of 100 ms (to shorten the test execution
    refresher.start(hikariConfig, 100, TimeUnit.MILLISECONDS);

    // then there shall be a call to RDS service to generate new token periodically
    // and the token shall be set in the HikariConfigMXBean
    await().atMost(Durations.ONE_SECOND).untilAsserted(() -> assertThat(hikariConfig.getPassword()).isEqualTo(token));
    verify(rdsUtilities, times(1)).generateAuthenticationToken(any(GenerateAuthenticationTokenRequest.class));
  }

}
