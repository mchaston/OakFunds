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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.chaston.oakfunds.jdbc.ColumnDef;
import org.chaston.oakfunds.jdbc.RemoteDataStoreModule;
import org.chaston.oakfunds.jdbc.TableDef;
import org.chaston.oakfunds.storage.RecordTypeRegistryModule;
import org.chaston.oakfunds.util.Flags;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO(mchaston): write JavaDocs
 */
public class SchemaValidator {
  private final SchemaBuilder schemaBuilder;
  private final DataSource dataSource;

  public static void main(String[] args) throws SQLException {
    Flags.parse(args);
    Injector injector = Guice.createInjector(
        new RecordTypeRegistryModule(),
        new AllTypesModule(),
        new StorageManagementModule(),
        new RemoteDataStoreModule());
    injector.getInstance(SchemaValidator.class).validateSchema();
  }

  @Inject
  SchemaValidator(
      SchemaBuilder schemaBuilder,
      DataSource dataSource) {
    this.schemaBuilder = schemaBuilder;
    this.dataSource = dataSource;
  }

  Iterable<SchemaDiscrepancy> validateSchema() throws SQLException {
    ImmutableMap<String, TableDef> tableDefs = schemaBuilder.getTableDefs();
    ImmutableList.Builder<SchemaDiscrepancy> schemaDiscrepancies = ImmutableList.builder();
    Set<String> seenTables = new HashSet<>();
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet allTables = metaData.getTables(null, null, null, null)) {
        while (allTables.next()) {
          String tableName = allTables.getString("TABLE_NAME").toLowerCase();
          TableDef tableDef = tableDefs.get(tableName);
          if (tableDef == null) {
            // Table that the defs do not know and hence care) about.
            continue;
          }
          seenTables.add(tableName);
          validateTable(metaData, tableDef, schemaDiscrepancies);
        }
      }
    }
    for (TableDef expectedTable : tableDefs.values()) {
      if (!seenTables.contains(expectedTable.getName())) {
        schemaDiscrepancies.add(new MissingTable(expectedTable));
      }
    }
    return schemaDiscrepancies.build();
  }

  private void validateTable(DatabaseMetaData metaData, TableDef tableDef,
      ImmutableList.Builder<SchemaDiscrepancy> schemaDiscrepancies) throws SQLException {
    Set<String> seenColumns = new HashSet<>();
    String upperTableName = tableDef.getName().toUpperCase();
    try (ResultSet columns = metaData.getColumns(null, null, upperTableName, null)) {
      while (columns.next()) {
        String columnName = columns.getString("COLUMN_NAME").toLowerCase();
        ColumnDef columnDef = tableDef.getColumnDefs().get(columnName);
        if (columnDef == null) {
          schemaDiscrepancies.add(new ExtraColumn(tableDef.getName(), columnName));
          continue;
        }
        seenColumns.add(columnName);
        // TODO: validate column type
        // TODO: validate whether column is required
      }
    }
    for (ColumnDef expectedColumn : tableDef.getColumnDefs().values()) {
      if (!seenColumns.contains(expectedColumn.getName())) {
        schemaDiscrepancies.add(new MissingColumn(tableDef.getName(), expectedColumn));
      }
    }
  }
}
