/*
 * Copyright © 2022 Lucas Persson. All Rights Reserved.
 */
package se.solrike.awsrdsiamdatasourcefactory.sample;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

import se.solrike.awsrdsiamdatasourcefactory.AbstractRdsIamDatasourceFactory;
import software.amazon.awssdk.services.rds.RdsClient;

/**
 * Datasource factory that supports IAM DB authentication when using Hikari connection pool.
 */
@Configuration
public class RdsIamDatasourceFactoryForSpring extends AbstractRdsIamDatasourceFactory<DataSourceProperties> {

  private final boolean mEnableIamAuthentication;
  private final int mRefreshIntervalInMinutes;

  // CHECKSTYLE:OFF
  @Autowired
  public RdsIamDatasourceFactoryForSpring(
      @Value("${spring.datasource.hikari.data-source-properties.iam-authentication:false}") boolean enableIamAuthentication,
      @Value("${spring.datasource.hikari.data-source-properties.iam-authentication-token-refresh-interval-in-minutes:14}") int refreshIntervalInMinutes) {
    mEnableIamAuthentication = enableIamAuthentication;
    mRefreshIntervalInMinutes = refreshIntervalInMinutes;
  }
  // CHECKSTYLE:ON

  /**
   *
   * The extra properties are added to the generic "data-source-properties:" section on the "spring.datasource.hikari"
   * section.
   * <p>
   *
   * <pre class="code">
   * <code class="yaml">
   * spring.datasource.url: jdbc:mysql://database1.crux4711.eu-north-1.rds.amazonaws.com:3306/database1
   * spring.datasource.username: myAppDbUser
   * spring.datasource.password:
   * spring.datasource.driver-class-name: com.mysql.cj.jdbc.Driver
   * spring.datasource.hikari.data-source-properties.iam-authentication: true
   * spring.datasource.hikari.data-source-properties.iam-authentication-token-refresh-interval-in-minutes: 14
   * </code>
   * </pre>
   *
   * @param dataSourceProperties
   *          - Spring datasource properties
   * @return datasource
   */
  @Primary
  @Bean
  // tells Spring to apply bindings on the created bean using the properties under "spring.datasource.hikar"
  @ConfigurationProperties(prefix = "spring.datasource.hikari")
  public DataSource dataSource(DataSourceProperties dataSourceProperties) {
    if (mEnableIamAuthentication) {
      try (RdsClient rdsClient = RdsClient.create()) {
        return createDatasource(rdsClient.utilities(), dataSourceProperties.getUrl(),
            dataSourceProperties.getUsername(), mRefreshIntervalInMinutes, dataSourceProperties);
      }
    }
    else {
      return doCreateDatasource(dataSourceProperties);
    }
  }

  @Override
  protected HikariDataSource doCreateDatasource(DataSourceProperties datasourceConfiguration) {
    HikariDataSource datasource = datasourceConfiguration.initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();

    // Apply Hikari specific properties if we need the properties here.
//    Binder binder = Binder.get(mEnvironment);
//    binder.bind("spring.datasource.hikari", Bindable.ofInstance(datasource).withExistingValue(datasource));
    return datasource;
  }

  @Override
  protected void setPassword(String passowrd, DataSourceProperties datasourceConfiguration) {
    datasourceConfiguration.setPassword(passowrd);
  }

}
