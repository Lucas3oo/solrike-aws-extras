/*
 * Copyright © 2022 Lucas Persson. All Rights Reserved.
 */
package se.solrike.aws.extras.datasourcefactory;

import java.net.URI;

import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.services.rds.RdsUtilities;

/**
 * Abstract datasource factory that can be customized for Spring and Micronaut etc way to create datasources that is
 * using IAM authentication against the AWS RDS database.
 */
@SuppressWarnings("java:S1610")
public abstract class AbstractRdsIamDatasourceFactory<C> {

  /**
   * Create the datasource and but first reconfigure it to support IAM DB authentication and enable password/token
   * refresh using Hikari's MXBean.
   *
   * @param rdsUtilities
   *          - AWS SDK utilities class. Get it from RdsClient.create().utilities()
   * @param jdbcUrl
   *          - e.g. jdbc:mysql://database1.crux4711.eu-north-1.rds.amazonaws.com:3306/database1. The hostname must be
   *          know to AWS RDS so it can't be from a private DNS. The URL must also have a port defined.
   * @param dbUsername
   *          - database username for the application
   * @param refreshIntervalInMinutes
   *          - auth token refresh interval. Token only valid for 15 min so it must be refreshed like every 14 min.
   * @param datasourceConfiguration
   *          - datasource configuration properties.
   * @return a datasource
   */
  protected HikariDataSource createDatasource(RdsUtilities rdsUtilities, String jdbcUrl, String dbUsername,
      int refreshIntervalInMinutes, C datasourceConfiguration) {

    String dbHostname = getUriFromJdbcUrl(jdbcUrl).getHost();
    int dbPort = getUriFromJdbcUrl(jdbcUrl).getPort();

    RdsIamAuthenticationTokenRefresher refresher = new RdsIamAuthenticationTokenRefresher(rdsUtilities, dbHostname,
        dbPort, dbUsername);

    // unfortunately the Hikari datasource tries to connect to the DB directly when it is created so the token needs
    // to be generated from start in case of Micronaut
    setPassword(refresher.getAuthenticationToken(), datasourceConfiguration);

    HikariDataSource datasource = doCreateDatasource(datasourceConfiguration);
    refresher.start(datasource.getHikariConfigMXBean(), refreshIntervalInMinutes);
    return datasource;

  }

  /**
   * Actually create the datasource using the platform (e.g. Spring/Micronaut etc) specific factory.
   *
   * @param datasourceConfiguration
   *          - datasource configuration properties.
   * @return datasource
   */
  protected abstract HikariDataSource doCreateDatasource(C datasourceConfiguration);

  /**
   * Update the password on the configuration.
   *
   * @param password
   *          - database password which will be a generated authentication token.
   * @param datasourceConfiguration
   *          - datasource configuration properties.
   */
  protected abstract void setPassword(String password, C datasourceConfiguration);

  private URI getUriFromJdbcUrl(String url) {
    // peal off 'jdbc:' so the parser works
    return URI.create(url.substring(5));
  }

}
