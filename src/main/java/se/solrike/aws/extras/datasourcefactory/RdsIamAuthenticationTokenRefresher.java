/*
 * Copyright © 2022 Lucas Persson. All Rights Reserved.
 */
package se.solrike.aws.extras.datasourcefactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfigMXBean;

import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

/**
 * Class that takes care to generates a new authentication token at regular intervals and updates the Hikari connection
 * pool with the new token in the password settings of the datasource configuration.
 */
public class RdsIamAuthenticationTokenRefresher {
  private static final Logger sLogger = LoggerFactory.getLogger(RdsIamAuthenticationTokenRefresher.class);

  private final RdsUtilities mRdsUtilities;
  private final GenerateAuthenticationTokenRequest mRequest;

  /**
   *
   * @param rdsUtilities
   *          - AWS SDK utilities class. Get it from RdsClient.create().utilities()
   * @param dbHostname
   *          - must be a hostname know to AWS RDS so it can't be from a private DNS.
   * @param dbPort
   *          - database port
   * @param dbUsername
   *          - database username for the application
   */
  public RdsIamAuthenticationTokenRefresher(RdsUtilities rdsUtilities, String dbHostname, int dbPort,
      String dbUsername) {
    mRdsUtilities = rdsUtilities;
    mRequest = GenerateAuthenticationTokenRequest.builder()
        .hostname(dbHostname)
        .port(dbPort)
        .username(dbUsername)
        .build();
  }

  /**
   * Can be used to get the initial token before the datasource is created.
   *
   * @return a token
   */
  public String getAuthenticationToken() {
    return mRdsUtilities.generateAuthenticationToken(mRequest);
  }

  /**
   * Start the token refresher.
   *
   * @param hikariConfigMxBean
   *          - from the Hikari datasource instance.
   * @param refreshIntervalInMinutes
   *          - preferred to be 14 min. Token expires after 15 min and there is no point to fetch it more often since
   *          there is also a limit on how many request can be done.
   */
  public void start(HikariConfigMXBean hikariConfigMxBean, int refreshIntervalInMinutes) {
    start(hikariConfigMxBean, refreshIntervalInMinutes, TimeUnit.MINUTES);
  }

  /**
   * Start the token refresher.
   *
   * @param hikariConfigMxBean
   *          - from the Hikari datasource instance.
   * @param refreshInterval
   *          - preferred to be 14 min. Token expires after 15 min and there is no point to fetch it more often since
   *          there is also a limit on how many request can be done.
   * @param timeUnit
   *          - time durations at a given unit of granularity
   */
  public void start(HikariConfigMXBean hikariConfigMxBean, int refreshInterval, TimeUnit timeUnit) {
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    Runnable task = () -> {
      sLogger.debug("Generate new authentication token for database pool '{}'", hikariConfigMxBean.getPoolName());
      hikariConfigMxBean.setPassword(getAuthenticationToken());
    };
    scheduler.scheduleWithFixedDelay(task, refreshInterval, refreshInterval, timeUnit);
  }

}
