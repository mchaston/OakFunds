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
import org.chaston.oakfunds.jdbc.DatabaseVariantHandler;
import org.chaston.oakfunds.jdbc.FunctionDef;
import org.chaston.oakfunds.jdbc.RemoteDataStoreModule;
import org.chaston.oakfunds.jdbc.TableDef;
import org.chaston.oakfunds.storage.RecordTypeRegistryModule;
import org.chaston.oakfunds.storage.SystemColumnDefs;
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
  private final DatabaseVariantHandler databaseVariantHandler;

  public static void main(String[] args) throws SQLException {
    Flags.parse(args);
    Injector injector = Guice.createInjector(
        new RecordTypeRegistryModule(),
        new AllTypesModule(),
        new StorageManagementModule(),
        new RemoteDataStoreModule());
    injector.getInstance(SchemaValidator.class).validateSchema();

    System.out.println("** Schema validated successfully. **");
  }

  @Inject
  SchemaValidator(
      SchemaBuilder schemaBuilder,
      DataSource dataSource,
      DatabaseVariantHandler databaseVariantHandler) {
    this.schemaBuilder = schemaBuilder;
    this.dataSource = dataSource;
    this.databaseVariantHandler = databaseVariantHandler;
  }

  Iterable<SchemaDiscrepancy> validateSchema() throws SQLException {
    ImmutableMap<String, TableDef> tableDefs = schemaBuilder.getTableDefs();
    ImmutableMap<String, FunctionDef> functionDefs = schemaBuilder.getFunctionDefs();
    ImmutableList.Builder<SchemaDiscrepancy> schemaDiscrepancies = ImmutableList.builder();
    Set<String> seenTables = new HashSet<>();
    Set<String> seenFunctions = new HashSet<>();
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();

      if (databaseVariantHandler.requiresSchemaCreation()) {
        // Look for schema.
        ResultSet allSchemas = metaData.getSchemas(null,
            databaseVariantHandler.toDatabaseForm(SystemColumnDefs.SCHEMA));
        if (!allSchemas.next()) {
          schemaDiscrepancies.add(new MissingSchema(SystemColumnDefs.SCHEMA));
        }
      }

      // Look for tables.
      try (ResultSet allTables = metaData.getTables(null,
          databaseVariantHandler.toDatabaseForm(SystemColumnDefs.SCHEMA), null, null)) {
        while (allTables.next()) {
          String tableName =
              databaseVariantHandler.toNormalName(allTables.getString("TABLE_NAME"));
          TableDef tableDef = tableDefs.get(tableName);
          if (tableDef == null) {
            // Table that the defs do not know (and hence care) about.
            continue;
          }
          seenTables.add(tableName);
          validateTable(metaData, tableDef, schemaDiscrepancies);
        }
      }

      // Look for functions.
      try (ResultSet allFunctions = metaData.getFunctions(null,
          databaseVariantHandler.toDatabaseForm(SystemColumnDefs.SCHEMA), null)) {
        while (allFunctions.next()) {
          String functionName =
              databaseVariantHandler.toNormalName(allFunctions.getString("FUNCTION_NAME"));
          FunctionDef functionDef = functionDefs.get(functionName);
          if (functionDef == null) {
            // Function that the defs do not know (and hence care) about.
            continue;
          }
          seenFunctions.add(functionName);
          // TODO: validate function (?)
        }
      }
    }
    for (TableDef expectedTable : tableDefs.values()) {
      if (!seenTables.contains(expectedTable.getName())) {
        schemaDiscrepancies.add(new MissingTable(expectedTable));
      }
    }
    for (FunctionDef expectedFunction : functionDefs.values()) {
      if (!seenFunctions.contains(expectedFunction.getName())) {
        schemaDiscrepancies.add(new MissingFunction(expectedFunction));
      }
    }
    return schemaDiscrepancies.build();
  }

  private void validateTable(DatabaseMetaData metaData, TableDef tableDef,
      ImmutableList.Builder<SchemaDiscrepancy> schemaDiscrepancies) throws SQLException {
    Set<String> seenColumns = new HashSet<>();
    String schemaName = databaseVariantHandler.toDatabaseForm(SystemColumnDefs.SCHEMA);
    String tableName = databaseVariantHandler.toDatabaseForm(tableDef.getName());
    try (ResultSet columns = metaData.getColumns(null, schemaName, tableName, null)) {
      while (columns.next()) {
        String columnName =
            databaseVariantHandler.toNormalName(columns.getString("COLUMN_NAME"));
        ColumnDef columnDef = tableDef.getColumnDefs().get(columnName);
        if (columnDef == null) {
          schemaDiscrepancies.add(new ExtraColumn(tableDef.getFullName(), columnName));
          continue;
        }
        seenColumns.add(columnName);
        // TODO: validate column type
        // TODO: validate whether column is required
      }
    }
    for (ColumnDef expectedColumn : tableDef.getColumnDefs().values()) {
      if (!seenColumns.contains(expectedColumn.getName())) {
        schemaDiscrepancies.add(new MissingColumn(tableDef.getFullName(), expectedColumn));
      }
    }
  }
}
