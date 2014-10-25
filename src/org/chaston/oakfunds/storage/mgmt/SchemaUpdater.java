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
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.chaston.oakfunds.jdbc.ColumnDef;
import org.chaston.oakfunds.jdbc.FunctionDef;
import org.chaston.oakfunds.jdbc.RemoteDataStoreModule;
import org.chaston.oakfunds.jdbc.TableDef;
import org.chaston.oakfunds.storage.RecordTypeRegistryModule;
import org.chaston.oakfunds.storage.SystemColumnDefs;
import org.chaston.oakfunds.util.Flags;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO(mchaston): write JavaDocs
 */
public class SchemaUpdater {

  private static final Logger logger = Logger.getLogger(SchemaUpdater.class.getName());

  private final SchemaValidator schemaValidator;
  private final DataSource dataSource;

  public static void main(String[] args) throws SQLException {
    Injector injector = Guice.createInjector(
        new RecordTypeRegistryModule(),
        new AllTypesModule(),
        new StorageManagementModule(),
        new RemoteDataStoreModule());
    Flags.parse(args);
    injector.getInstance(SchemaUpdater.class).updateSchema();
  }

  @Inject
  SchemaUpdater(
      SchemaValidator schemaValidator,
      DataSource dataSource) {
    this.schemaValidator = schemaValidator;
    this.dataSource = dataSource;
  }

  Iterable<SchemaDiscrepancy> updateSchema() throws SQLException {
    Iterable<SchemaDiscrepancy> discrepancies = schemaValidator.validateSchema();
    try (Connection connection = dataSource.getConnection()) {
      createMissingSchemas(connection,
          Iterables.filter(discrepancies, MissingSchema.class));
      createMissingTables(connection,
          Iterables.filter(discrepancies, MissingTable.class));
      handleTableDefDiscrepancies(connection,
          Iterables.filter(discrepancies, TableDefDiscrepancy.class));
      createMissingFunctions(connection,
          Iterables.filter(discrepancies, MissingFunction.class));
    }

    // Re-validate when done.
    return schemaValidator.validateSchema();
  }

  private void createMissingSchemas(Connection connection, Iterable<MissingSchema> missingSchemas)
      throws SQLException {
    for (MissingSchema missingSchema : missingSchemas) {
      StringBuilder createSchemaStatement = new StringBuilder();
      createSchemaStatement.append("CREATE SCHEMA ").append(missingSchema.getName()).append(";");
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate(createSchemaStatement.toString());
        logger.info("Schema " + missingSchema.getName() + " was created.");
      } catch (SQLException e) {
        logger.log(Level.SEVERE, "Failure to execute: " + createSchemaStatement.toString(), e);
        throw e;
      }
    }
  }

  private void createMissingTables(Connection connection,
      Iterable<MissingTable> discrepancies) throws SQLException {
    for (MissingTable discrepancy : discrepancies) {
      TableDef missingTable = discrepancy.getTableDef();
      StringBuilder createTableStatement = new StringBuilder();
      createTableStatement.append("CREATE TABLE ").append(missingTable.getFullName()).append(" (");
      for (ColumnDef columnDef : missingTable.getColumnDefs().values()) {
        appendColumnDeclaration(createTableStatement, columnDef);
        createTableStatement.append(",\n");
      }
      createTableStatement.append("PRIMARY KEY ( ")
          .append(SystemColumnDefs.ID_COLUMN_NAME).append(" )\n");
      createTableStatement.append(");");
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate(createTableStatement.toString());
        logger.info("Table " + missingTable.getName() + " was created.");
      } catch (SQLException e) {
        logger.log(Level.SEVERE, "Failure to execute: " + createTableStatement.toString(), e);
        throw e;
      }
    }
  }

  private void appendColumnDeclaration(StringBuilder statement, ColumnDef columnDef) {
    statement.append(columnDef.getName()).append(' ');
    statement.append(toSqlTypeString(columnDef));
    if (columnDef.isRequired()) {
      statement.append(" NOT NULL");
    }
    if (columnDef.isAutoNumbered()) {
      statement.append(" AUTO_INCREMENT");
    }
  }

  private String toSqlTypeString(ColumnDef columnDef) {
    switch (columnDef.getType()) {
      case Types.INTEGER:
        return "INT";
      case Types.TIMESTAMP:
        return "TIMESTAMP";
      case Types.SMALLINT:
        return "SMALLINT";
      case Types.BIGINT:
        return "BIGINT";
      case Types.BOOLEAN:
        return "TINYINT";
      case Types.VARCHAR:
        // TODO: make this size aware
        return "VARCHAR(255)";
      default:
        throw new UnsupportedOperationException("Column type " + columnDef.getType()
            + " used for column " + columnDef.getName() + " is not supported.");
    }
  }

  private void handleTableDefDiscrepancies(Connection connection,
      Iterable<TableDefDiscrepancy> discrepancies) throws SQLException {
    Map<String, Collection<TableDefDiscrepancy>> discrepanciesByTable = groupByTable(discrepancies);
    for (Map.Entry<String, Collection<TableDefDiscrepancy>> entry
        : discrepanciesByTable.entrySet()) {
      String tableName = entry.getKey();
      for (TableDefDiscrepancy tableDefDiscrepancy : entry.getValue()) {
        // MySQL supports multiple actions per ALTER TABLE, but others do not, so we should
        // alter tables one piece at a time.
        StringBuilder alterTableStatement = new StringBuilder();
        alterTableStatement.append("ALTER TABLE ").append(tableName).append(" ");
        if (tableDefDiscrepancy instanceof MissingColumn) {
          MissingColumn missingColumn = (MissingColumn) tableDefDiscrepancy;
          alterTableStatement.append("ADD COLUMN ");
          appendColumnDeclaration(alterTableStatement, missingColumn.getColumnDef());
          logger.info("Column " + missingColumn.getTableName() + "."
              + missingColumn.getColumnDef().getName() + " was added.");
        } else if (tableDefDiscrepancy instanceof ExtraColumn) {
          // Columns are not automatically dropped as this is dangerous.
          ExtraColumn extraColumn = (ExtraColumn) tableDefDiscrepancy;
          logger.warning("Extra column " + extraColumn.getTableName() + "."
              + extraColumn.getColumnName() + " was detected.");
          continue;
        } else {
          throw new UnsupportedOperationException(
              "Discrepancies of type " + tableDefDiscrepancy.getClass().getName()
                  + " are not supported.");
        }
        alterTableStatement.append(";");
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate(alterTableStatement.toString());
        } catch (SQLException e) {
          logger.log(Level.SEVERE, "Failure to execute: " + alterTableStatement.toString(), e);
          throw e;
        }
      }
    }
  }

  private Map<String, Collection<TableDefDiscrepancy>> groupByTable(
      Iterable<TableDefDiscrepancy> discrepancies) {
    Multimap<String, TableDefDiscrepancy> discrepanciesByTable =
        MultimapBuilder.hashKeys().arrayListValues().build();
    for (TableDefDiscrepancy discrepancy : discrepancies) {
      discrepanciesByTable.put(discrepancy.getTableName(), discrepancy);
    }
    return discrepanciesByTable.asMap();
  }

  private void createMissingFunctions(Connection connection,
      Iterable<MissingFunction> missingFunctions) throws SQLException {
    DatabaseMetaData metadata = connection.getMetaData();
    FunctionSelector functionSelector = determineFunctionSelector(metadata);
    for (MissingFunction missingFunction : missingFunctions) {
      String createFunctionSql = functionSelector.functionContent(missingFunction.getFunctionDef());
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate(createFunctionSql);
        logger.info("Function " + missingFunction.getFunctionDef().getName() + " was created.");
      } catch (SQLException e) {
        logger.log(Level.SEVERE, "Failure to execute: " + createFunctionSql, e);
        throw e;
      }
    }
  }

  private FunctionSelector determineFunctionSelector(DatabaseMetaData metadata)
      throws SQLException {
    String databaseProduct = metadata.getDatabaseProductName();
    if (databaseProduct.equalsIgnoreCase("HSQL Database Engine")) {
      return new FunctionSelector() {
        @Override
        public String functionContent(FunctionDef functionDef) {
          return functionDef.getHsqldbFunction();
        }
      };
    }
    if (databaseProduct.equalsIgnoreCase("MySQL")) {
      return new FunctionSelector() {
        @Override
        public String functionContent(FunctionDef functionDef) {
          return functionDef.getMysqlFunction();
        }
      };
    }
    throw new UnsupportedOperationException("Functions cannot be selected for " + databaseProduct);
  }

  private static interface FunctionSelector {
    String functionContent(FunctionDef functionDef);
  }
}
