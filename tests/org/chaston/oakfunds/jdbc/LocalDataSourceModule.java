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
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import org.hsqldb.jdbc.JDBCDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO(mchaston): write JavaDocs
 */
public class LocalDataSourceModule extends AbstractModule {

  private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();
  private final int instance = INSTANCE_COUNTER.incrementAndGet();

  @Override
  protected void configure() {
    bind(DatabaseTearDown.class).in(Scopes.SINGLETON);
    bind(DatabaseVariantHandler.class).to(HsqlDbDatabaseVariantHandler.class);
  }

  @Provides
  @Singleton
  DataSource provideDataSource() throws SQLException {
    JDBCDataSource dataSource = new JDBCDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:test_" + instance);
    Properties props = new Properties();

    // Require explict shutdown.
    props.put("shutdown", "false");

    // Make it look like MySQL
    props.put("sql.syntax_mys", "true");

    // Use strict configurations.
    props.put("sql.enforce_names", "true");
    props.put("sql.enforce_refs", "true");
    props.put("sql.enforce_types", "true");

    dataSource.setProperties(props);

    return dataSource;
  }

  private static class HsqlDbDatabaseVariantHandler implements DatabaseVariantHandler {
    @Override
    public String toDatabaseForm(String normalName) {
      return normalName.toUpperCase();
    }

    @Override
    public String toNormalName(String databaseForm) {
      return databaseForm.toLowerCase();
    }

    @Override
    public boolean requiresSchemaCreation() {
      return true;
    }
  }
}
