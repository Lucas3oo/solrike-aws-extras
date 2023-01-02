/*
 * Copyright © 2022 Lucas Persson. All Rights Reserved.
 */
package se.solrike.aws.extras.datasourcefactory.sample;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import se.solrike.aws.extras.datasourcefactory.AbstractRdsIamDatasourceFactory;
import software.amazon.awssdk.services.rds.RdsUtilities;

/**
 * Generic implementation to demonstrate the IAM authentication integration with Hikari.
 */
public class GenericRdsIamDatasourceFactory extends AbstractRdsIamDatasourceFactory<HikariConfig> {

  private final RdsUtilities mRdsUtilities;

  public GenericRdsIamDatasourceFactory(RdsUtilities rdsUtilities) {
    mRdsUtilities = rdsUtilities;
  }

  public HikariDataSource dataSource(HikariConfig datasourceConfiguration) {
    int refreshIntervalInMinutes = (int) datasourceConfiguration.getDataSourceProperties()
        .getOrDefault("iam-authentication-token-refresh-interval-in-minutes", 14);
    return createDatasource(mRdsUtilities, datasourceConfiguration.getJdbcUrl(), datasourceConfiguration.getUsername(),
        refreshIntervalInMinutes, datasourceConfiguration);
  }

  @Override
  protected HikariDataSource doCreateDatasource(HikariConfig datasourceConfiguration) {
    // create the datasource but do not start the pool. It will be started at first call to getConnection().
    // it is better to try to connect directly in order to fail fast if something isn't correctly configured.
    HikariDataSource hikariDataSource = new HikariDataSource();
    datasourceConfiguration.copyStateTo(hikariDataSource);
    return hikariDataSource;
  }

  @Override
  protected void setPassword(String password, HikariConfig datasourceConfiguration) {
    datasourceConfiguration.setPassword(password);
  }

}
