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
package org.chaston.oakfunds.storage.mgmt;

import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.chaston.oakfunds.jdbc.LocalDataSourceModule;
import org.chaston.oakfunds.storage.RecordTypeRegistryModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class SchemaValidatorTest {

  @Inject private SchemaValidator schemaValidator;
  @Inject private DataSource dataSource;
  private Connection connection;

  @Before
  public void setUp() throws SQLException {
    Injector injector = Guice.createInjector(
        new StorageManagementModule(),
        new AllTypesModule(),
        new RecordTypeRegistryModule(),
        new LocalDataSourceModule());
    injector.injectMembers(this);
    connection = dataSource.getConnection();
  }

  @After
  public void tearDown() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  public void testValidateEmptySchema() throws SQLException {
    Iterable<SchemaDiscrepancy> discrepancies = schemaValidator.validateSchema();
    assertEquals(10, Iterables.size(Iterables.filter(discrepancies, MissingTable.class)));
    assertEquals(3, Iterables.size(Iterables.filter(discrepancies, MissingFunction.class)));
  }
}
