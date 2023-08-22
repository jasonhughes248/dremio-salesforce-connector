/*
 * Copyright (C) 2017-2018 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.exec.store.jdbc.conf;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hibernate.validator.constraints.NotBlank;

import com.dremio.exec.catalog.conf.DisplayMetadata;
import com.dremio.exec.catalog.conf.NotMetadataImpacting;
import com.dremio.exec.catalog.conf.Secret;
import com.dremio.exec.catalog.conf.SourceType;
import com.dremio.options.OptionManager;
import com.dremio.security.CredentialsService;
import com.dremio.exec.store.jdbc.CloseableDataSource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.dremio.exec.store.jdbc.DataSources;
import com.dremio.exec.store.jdbc.JdbcPluginConfig;
import com.dremio.exec.store.jdbc.dialect.arp.ArpDialect;
import com.google.common.annotations.VisibleForTesting;

import io.protostuff.Tag;

/**
 * Configuration for Salesforce sources.
 */
@SourceType(value = "SALESFORCEARP", label = "Salesforce", uiConfig = "salesforce-layout.json", externalQuerySupported = true)
public class SalesforceConf extends AbstractArpConf<SalesforceConf> {
  private static final String ARP_FILENAME = "arp/implementation/salesforce-arp.yaml";
  private static final ArpDialect ARP_DIALECT =
      AbstractArpConf.loadArpFile(ARP_FILENAME, (ArpDialect::new));
  private static final String DRIVER = "cdata.jdbc.salesforce.SalesforceDriver";

  @NotBlank
  @Tag(1)
  @DisplayMetadata(label = "Username")
  public String username;


  @NotBlank
  @Tag(2)
  @Secret
  @DisplayMetadata(label = "Password")
  public String password;



  @NotBlank
  @Tag(3)
  @Secret
  @DisplayMetadata(label = "Security Token")
  public String securityToken;



  @Tag(4)
  @DisplayMetadata(label = "Record fetch size")
  @NotMetadataImpacting
  public int fetchSize = 200;

  @Tag(5)
  @NotMetadataImpacting
  @JsonIgnore
  @DisplayMetadata(label = "Grant External Query access (External Query allows creation of VDS from a Salesforce query. Learn more here: https://docs.dremio.com/data-sources/external-queries.html#enabling-external-queries)")
  public boolean enableExternalQuery = false;

  @Tag(6)
  @DisplayMetadata(label = "Maximum idle connections")
  @NotMetadataImpacting
  public int maxIdleConns = 8;

  @Tag(7)
  @DisplayMetadata(label = "Connection idle time (s)")
  @NotMetadataImpacting
  public int idleTimeSec = 60;



  @VisibleForTesting
  public String toJdbcConnectionString() {
    final String username = checkNotNull(this.username, "Missing username.");
    final String password = checkNotNull(this.password, "Missing password.");
    final String securityToken = checkNotNull(this.securityToken, "Missing security token.");

    return String.format("jdbc:salesforce:User=%s;Password=%s;Security Token=%s", username, password, securityToken);
  }

  @Override
  @VisibleForTesting
  public JdbcPluginConfig buildPluginConfig(JdbcPluginConfig.Builder configBuilder, CredentialsService credentialsService, OptionManager optionManager) {
         return configBuilder.withDialect(getDialect())
        .withFetchSize(fetchSize)
        .withDatasourceFactory(this::newDataSource)
        .clearHiddenSchemas()
        //.addHiddenSchema("SYSTEM")
        .build();
  }

  private CloseableDataSource newDataSource() {
    return DataSources.newGenericConnectionPoolDataSource(DRIVER,
      toJdbcConnectionString(), username, password, null, DataSources.CommitMode.DRIVER_SPECIFIED_COMMIT_MODE,
      maxIdleConns,
      idleTimeSec);
    }

  @Override
  public ArpDialect getDialect() {
    return ARP_DIALECT;
  }

  @VisibleForTesting
  public static ArpDialect getDialectSingleton() {
    return ARP_DIALECT;
  }
}
