/*
 * Copyright © 2022 Lucas Persson. All Rights Reserved.
 */
package se.solrike.awsrdsiamdatasourcefactory.sample;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import io.micronaut.configuration.jdbc.hikari.DatasourceConfiguration;
import io.micronaut.configuration.jdbc.hikari.DatasourceFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import se.solrike.awsrdsiamdatasourcefactory.AbstractRdsIamDatasourceFactory;
import software.amazon.awssdk.services.rds.RdsClient;

/**
 * Datasource factory that supports IAM DB authentication when using Hikari connection pool.
 */
@Factory
@Replaces(factory = DatasourceFactory.class)
public class RdsIamDatasourceFactoryForMicronaut extends AbstractRdsIamDatasourceFactory<DatasourceConfiguration>
    implements AutoCloseable {

  private DatasourceFactory mDatasourceFactory;

  public RdsIamDatasourceFactoryForMicronaut(ApplicationContext applicationContext) {
    mDatasourceFactory = new DatasourceFactory(applicationContext);
  }

  /**
   * The extra properties are added to the generic "data-source-properties:" section on the named datasource under the
   * "datasources" section.
   * <p>
   *
   * <pre class="code">
   * <code class="yaml">
   * datasources:
   *   default:
   *     url: jdbc:mysql://database1.crux4711.eu-north-1.rds.amazonaws.com:3306/database1
   *     username: myAppDbUser
   *     password: ''
   *     driverClassName: com.mysql.cj.jdbc.Driver
   *     dialect: org.hibernate.dialect.MySQL8Dialect
   *     # hikari specific section
   *     data-source-properties:
   *       iam-authentication: true
   *       iam-authentication-token-refresh-interval-in-minutes: 14
   * </code>
   * </pre>
   *
   * @param datasourceConfiguration
   * @return datasource
   */
  @Context()
  @EachBean(DatasourceConfiguration.class)
  public DataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
    if ((boolean) datasourceConfiguration.getDataSourceProperties().getOrDefault("iam-authentication", false)) {
      int refreshIntervalInMinutes = (int) datasourceConfiguration.getDataSourceProperties()
          .getOrDefault("iam-authentication-token-refresh-interval-in-minutes", 14);
      try (RdsClient rdsClient = RdsClient.create()) {
        return createDatasource(rdsClient.utilities(), datasourceConfiguration.getUrl(),
            datasourceConfiguration.getConfiguredUsername(), refreshIntervalInMinutes, datasourceConfiguration);
      }
    }
    else {
      return doCreateDatasource(datasourceConfiguration);
    }
  }

  @Override
  protected HikariDataSource doCreateDatasource(DatasourceConfiguration datasourceConfiguration) {
    return (HikariDataSource) mDatasourceFactory.dataSource(datasourceConfiguration);
  }

  @Override
  protected void setPassword(String passowrd, DatasourceConfiguration datasourceConfiguration) {
    datasourceConfiguration.setPassword(passowrd);
  }

  @Override
  @PreDestroy
  public void close() {
    mDatasourceFactory.close();
  }

}
