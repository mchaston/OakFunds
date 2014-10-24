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
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.chaston.oakfunds.storage.SystemColumnDefs;
import org.chaston.oakfunds.util.Flag;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * TODO(mchaston): write JavaDocs
 */
public class RemoteDataStoreModule extends AbstractModule {

  private static final String URL_PATTERN = "jdbc:mysql://%s:3306/" + SystemColumnDefs.SCHEMA;

  private static final Flag<String> DB_ADDRESS =
      Flag.builder("db_address", "127.0.0.1")
          .build();

  private static final Flag<String> DB_USERNAME =
      Flag.builder("db_username", "root")
          .build();

  private static final Flag<String> DB_PASSWORD =
      Flag.builder("db_password", "xxxxx")
          .build();

  @Override
  protected void configure() {
    bind(DatabaseObjectNameHandler.class).to(MySqlDatabaseObjectNameHandler.class);
  }

  @Provides
  @Singleton
  DataSource provideDataSource() throws SQLException {
    MysqlDataSource dataSource = new MysqlDataSource();
    dataSource.setUrl(String.format(URL_PATTERN, DB_ADDRESS.get()));
    dataSource.setUser(DB_USERNAME.get());
    dataSource.setPassword(DB_PASSWORD.get());
    dataSource.setCreateDatabaseIfNotExist(true);
    return dataSource;
  }

  private static class MySqlDatabaseObjectNameHandler implements DatabaseObjectNameHandler {
    @Override
    public String toDatabaseForm(String normalName) {
      return normalName;
    }

    @Override
    public String toNormalName(String databaseForm) {
      return databaseForm;
    }
  }
}
