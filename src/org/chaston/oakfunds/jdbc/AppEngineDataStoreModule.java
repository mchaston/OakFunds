/*
 * Copyright 2014 Miles Chaston
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
package org.chaston.oakfunds.jdbc;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.commons.dbcp2.BasicDataSource;
import org.chaston.oakfunds.storage.SystemColumnDefs;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * TODO(mchaston): write JavaDocs
 */
public class AppEngineDataStoreModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DatabaseVariantHandler.class).to(MySqlDatabaseVariantHandler.class);
  }

  @Provides
  @Singleton
  DataSource provideDataSource() throws SQLException {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName("com.mysql.jdbc.GoogleDriver");
    dataSource.setUrl(
        "jdbc:google:mysql://mchaston-oakfunds:oakfunds2/" + SystemColumnDefs.SCHEMA);
    return dataSource;
  }
}
