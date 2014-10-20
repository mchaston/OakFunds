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
package org.chaston.oakfunds.storage;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.chaston.oakfunds.jdbc.ColumnDef;
import org.chaston.oakfunds.util.DateUtil;
import org.joda.time.Instant;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO(mchaston): write JavaDocs
 */
class StoreImpl implements Store {

  private static final Logger logger = Logger.getLogger(StoreImpl.class.getName());
  private final ThreadLocal<TransactionImpl> CURRENT_TRANSACTION = new ThreadLocal<>();

  private final DataSource dataSource;
  private final RecordTypeRegistry recordTypeRegistry;

  @Inject
  StoreImpl(DataSource dataSource,
      RecordTypeRegistry recordTypeRegistry) {
    this.dataSource = dataSource;
    this.recordTypeRegistry = recordTypeRegistry;
  }

  @Override
  public Transaction startTransaction() throws StorageException {
    if (CURRENT_TRANSACTION.get() != null) {
      throw new IllegalStateException("Already in transaction.");
    }
    TransactionImpl newTransaction = null;
    try {
      newTransaction = new TransactionImpl(this, getNewConnection());
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to start new transaction", e);
    }
    CURRENT_TRANSACTION.set(newTransaction);
    return newTransaction;
  }

  void endTransaction(Connection connection) {
    CURRENT_TRANSACTION.remove();
    closeConnection(connection);
  }

  @Override
  public <T extends Record> T createRecord(RecordType<T> recordType, int id,
      Map<String, Object> attributes) throws StorageException {
    recordTypeRegistry.validateRecordAttributes(recordType, attributes);
    TransactionImpl currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    insertRecord(currentTransaction.getConnection(), recordType, id, attributes);
    return getRecord(recordType, id);
  }

  private <T extends Record> void insertRecord(Connection connection,
      RecordType<T> recordType, int id, Map<String, Object> attributes) throws StorageException {
    Preconditions.checkArgument(!recordType.isAutoIncrementId(),
        "You cannot specify an ID for an auto-incrementing record type.");

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("INSERT INTO ").append(recordType.getTableName()).append("(");
    stringBuilder.append(SystemColumnDefs.ID_COLUMN_NAME).append(", ");
    stringBuilder.append(SystemColumnDefs.TYPE.getName()).append(", ");
    appendAttributeColumnNames(stringBuilder, recordType, attributes);
    stringBuilder.append(") VALUES (");
    appendQuestionMarks(stringBuilder, attributes.size() + 2);
    stringBuilder.append(");");

    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
      stmt.setInt(1, id);
      stmt.setString(2, recordType.getName());
      setParameters(recordType, stmt, 3, attributes);
      stmt.executeUpdate();
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to insert record of type " + recordType.getName(), e);
      throw new StorageException("Failed to insert record of type " + recordType.getName(), e);
    }
  }

  @Override
  public <T extends Record> T createRecord(RecordType<T> recordType, Map<String, Object> attributes)
      throws StorageException {
    recordTypeRegistry.validateRecordAttributes(recordType, attributes);
    TransactionImpl currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    int newId = insertRecord(currentTransaction.getConnection(), recordType, attributes);
    return getRecord(recordType, newId);
  }

  private <T extends Record> int insertRecord(Connection connection,
      RecordType<T> recordType, Map<String, Object> attributes) throws StorageException {
    Preconditions.checkArgument(recordType.isAutoIncrementId(),
        "You must specify an ID for a manually identifying record type.");

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("INSERT INTO ").append(recordType.getTableName()).append("(");
    stringBuilder.append(SystemColumnDefs.TYPE.getName()).append(", ");
    appendAttributeColumnNames(stringBuilder, recordType, attributes);
    stringBuilder.append(") VALUES (");
    appendQuestionMarks(stringBuilder, attributes.size() + 1);
    stringBuilder.append(");");

    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString(),
        Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, recordType.getName());
      setParameters(recordType, stmt, 2, attributes);
      stmt.executeUpdate();
      // Get the generated ID back.
      ResultSet tableKeys = stmt.getGeneratedKeys();
      if (!tableKeys.next()) {
        logger.log(Level.WARNING,
            "Failed to get ID back after insert for type " + recordType.getName());
        throw new StorageException(
            "Failed to get ID back after insert for type " + recordType.getName());
      }
      return tableKeys.getInt(1);
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to insert record of type " + recordType.getName(), e);
      throw new StorageException("Failed to insert record of type " + recordType.getName(), e);
    }
  }

  @Override
  public <T extends Record> T getRecord(RecordType<T> recordType, int id) throws StorageException {
    try (ReadingDataSource readingDataSource = new ReadingDataSource()) {
      RawRecord<T> rawRecord = getRecord(readingDataSource.getConnection(), recordType, id);
      return RecordProxy.proxyRecord(rawRecord.getRecordType(),
          null, id, rawRecord.getAttributes());
    }
  }

  private <T extends Record> RawRecord<T> getRecord(Connection connection,
      RecordType<T> recordType, int id) throws StorageException {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("SELECT * FROM ").append(recordType.getTableName());
    stringBuilder.append(" WHERE ").append(SystemColumnDefs.ID_COLUMN_NAME).append(" = ?;");

    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          logger.log(Level.WARNING,
              "Failed to read record " + id + " for type " + recordType.getName());
          throw new StorageException(
              "Failed to read record " + id + " for type " + recordType.getName());
        }
        RecordType<T> loadedRecordType = loadedRecordType(rs, recordType);
        return new RawRecord<>(loadedRecordType, id, readAttributes(loadedRecordType, rs));
      }
    } catch (SQLException e) {
      logger.log(Level.WARNING,
          "Failed to read record " + id + " for type " + recordType.getName(), e);
      throw new StorageException(
          "Failed to read record " + id + " for type " + recordType.getName(), e);
    }
  }

  private <T extends Record> RecordType<T> loadedRecordType(
      ResultSet rs, RecordType<T> recordType) throws SQLException {
    return recordTypeRegistry.getType(rs.getString(SystemColumnDefs.TYPE.getName()), recordType);
  }

  @Override
  public <T extends Record> T updateRecord(T record, Map<String, Object> attributes)
      throws StorageException {
    recordTypeRegistry.validateRecordAttributes(record.getRecordType(), attributes);
    TransactionImpl currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    updateRecord(currentTransaction.getConnection(),
        record.getRecordType(), record.getId(), attributes);
    return (T) getRecord(record.getRecordType(), record.getId());
  }

  private <T extends Record> void updateRecord(Connection connection,
      RecordType<T> recordType, int id, Map<String, Object> attributes) throws StorageException {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("UPDATE ").append(recordType.getTableName()).append(" SET ");
    Joiner.on(" = ?, ").appendTo(stringBuilder, prefixColumnNames(recordType, attributes.keySet()));
    stringBuilder.append(" = ?");
    stringBuilder.append(" WHERE ").append(SystemColumnDefs.ID_COLUMN_NAME).append(" = ?;");

    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
      int nextIndex = setParameters(recordType, stmt, 1, attributes);
      stmt.setInt(nextIndex, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to insert record of type " + recordType.getName(), e);
      throw new StorageException("Failed to insert record of type " + recordType.getName(), e);
    }
  }

  @Override
  public <T extends IntervalRecord> T updateIntervalRecord(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end, Map<String, Object> attributes)
      throws StorageException {
    recordTypeRegistry.validateRecordAttributes(recordType, attributes);
    TransactionImpl currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    int id = updateIntervalRecord(currentTransaction.getConnection(),
        containingRecord, recordType, start, end, attributes);
    return RecordProxy.proxyIntervalRecord(recordType,
        containingRecord, id, start, end, attributes);
  }

  private <T extends IntervalRecord> int updateIntervalRecord(Connection connection,
      Record containingRecord, RecordType<T> recordType, Instant start, Instant end,
      Map<String, Object> attributes) throws StorageException {
    // Load any record that starts before start and ends after start.
    RawIntervalRecord<T> overlapping =
        getIntervalRecord(connection, containingRecord, recordType, start);
    if (overlapping != null) {
      // If it ends after end, clone it to create a new one.
      if (overlapping.getEnd().isAfter(end)) {
        insertIntervalRecord(connection, containingRecord.getId(), overlapping.getRecordType(),
            end, overlapping.getEnd(), overlapping.getAttributes());
      }
      // Truncate the original.
      truncateIntervalRecord(connection, overlapping, SystemColumnDefs.END_TIME, start);
    }
    // Delete all records that start on or after start and end before end.
    deleteIntervalRecords(connection, containingRecord, recordType, start, end);
    // Truncate any record that starts on or after start and ends after end.
    overlapping = getIntervalRecord(connection, containingRecord, recordType, end.minus(1));
    if (overlapping != null) {
      truncateIntervalRecord(connection, overlapping, SystemColumnDefs.START_TIME, end);
    }
    // Insert new record.
    return insertIntervalRecord(connection, containingRecord.getId(), recordType,
        start, end, attributes);
  }

  private <T extends IntervalRecord> void deleteIntervalRecords(Connection connection,
      Record containingRecord, RecordType<T> recordType, Instant start, Instant end)
      throws StorageException {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("DELETE FROM ").append(recordType.getTableName());
    stringBuilder.append(" WHERE ");
    stringBuilder.append(SystemColumnDefs.CONTAINER_ID.getName()).append(" = ? AND ");
    stringBuilder.append(SystemColumnDefs.START_TIME.getName()).append(" >= ? AND ");
    stringBuilder.append(SystemColumnDefs.END_TIME.getName()).append(" < ?;");

    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
      stmt.setInt(1, containingRecord.getId());
      stmt.setTimestamp(2, getTimestamp(start));
      stmt.setTimestamp(3, getTimestamp(end));
      stmt.executeUpdate();
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to delete records of type " + recordType.getName(), e);
      throw new StorageException("Failed to update records of type " + recordType.getName(), e);
    }
  }

  private <T extends IntervalRecord> void truncateIntervalRecord(Connection connection,
      RawIntervalRecord<T> record, ColumnDef columnDef, Instant instant)
      throws StorageException {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("UPDATE ").append(record.getRecordType().getTableName());
    stringBuilder.append(" SET ").append(columnDef.getName()).append(" = ?");
    stringBuilder.append(" WHERE ").append(SystemColumnDefs.ID_COLUMN_NAME).append(" = ?;");

    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
      stmt.setTimestamp(1, getTimestamp(instant));
      stmt.setInt(2, record.getId());
      stmt.executeUpdate();
    } catch (SQLException e) {
      logger.log(Level.WARNING,
          "Failed to update record " + record.getId() + " of type "
              + record.getRecordType().getName(), e);
      throw new StorageException(
          "Failed to update record " + record.getId() + " of type "
              + record.getRecordType().getName(), e);
    }
  }

  private <T extends IntervalRecord> int insertIntervalRecord(Connection connection,
      int containingId, RecordType<T> recordType,
      Instant start, Instant end, Map<String, Object> attributes) throws StorageException{
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("INSERT INTO ").append(recordType.getTableName()).append("(");
    stringBuilder.append(SystemColumnDefs.TYPE.getName()).append(", ");
    stringBuilder.append(SystemColumnDefs.CONTAINER_ID.getName()).append(", ");
    stringBuilder.append(SystemColumnDefs.START_TIME.getName()).append(", ");
    stringBuilder.append(SystemColumnDefs.END_TIME.getName()).append(", ");
    appendAttributeColumnNames(stringBuilder, recordType, attributes);
    stringBuilder.append(") VALUES (");
    appendQuestionMarks(stringBuilder, attributes.size() + 4);
    stringBuilder.append(");");

    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString(),
        Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, recordType.getName());
      stmt.setInt(2, containingId);
      stmt.setTimestamp(3, getTimestamp(start));
      stmt.setTimestamp(4, getTimestamp(end));
      setParameters(recordType, stmt, 5, attributes);
      stmt.executeUpdate();
      // Get the generated ID back.
      ResultSet tableKeys = stmt.getGeneratedKeys();
      if (!tableKeys.next()) {
        logger.log(Level.WARNING,
            "Failed to get ID back after insert for type " + recordType.getName());
        throw new StorageException(
            "Failed to get ID back after insert for type " + recordType.getName());
      }
      return tableKeys.getInt(1);
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to insert record of type " + recordType.getName(), e);
      throw new StorageException("Failed to insert record of type " + recordType.getName(), e);
    }
  }

  @Override
  public <T extends InstantRecord> T insertInstantRecord(Record containingRecord,
      RecordType<T> recordType, Instant instant, Map<String, Object> attributes)
      throws StorageException {
    recordTypeRegistry.validateRecordAttributes(recordType, attributes);
    TransactionImpl currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    int id = insertInstantRecord(currentTransaction.getConnection(),
        containingRecord.getId(), recordType, instant, attributes);
    return RecordProxy.proxyInstantRecord(recordType,
        containingRecord, id, instant, attributes);
  }

  <T extends InstantRecord> int insertInstantRecord(Connection connection,
      int containingId, RecordType<T> recordType, Instant instant, Map<String, Object> attributes)
      throws StorageException {
    Preconditions.checkArgument(recordType.isAutoIncrementId(),
        "You must specify an ID for a manually identifying record type.");

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("INSERT INTO ").append(recordType.getTableName()).append("(");
    stringBuilder.append(SystemColumnDefs.TYPE.getName()).append(", ");
    stringBuilder.append(SystemColumnDefs.CONTAINER_ID.getName()).append(", ");
    stringBuilder.append(SystemColumnDefs.INSTANT.getName()).append(", ");
    appendAttributeColumnNames(stringBuilder, recordType, attributes);
    stringBuilder.append(") VALUES (");
    appendQuestionMarks(stringBuilder, attributes.size() + 3);
    stringBuilder.append(");");

    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString(),
        Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, recordType.getName());
      stmt.setInt(2, containingId);
      stmt.setTimestamp(3, getTimestamp(instant));
      setParameters(recordType, stmt, 4, attributes);
      stmt.executeUpdate();
      // Get the generated ID back.
      ResultSet tableKeys = stmt.getGeneratedKeys();
      if (!tableKeys.next()) {
        logger.log(Level.WARNING,
            "Failed to get ID back after insert for type " + recordType.getName());
        throw new StorageException(
            "Failed to get ID back after insert for type " + recordType.getName());
      }
      return tableKeys.getInt(1);
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to insert record of type " + recordType.getName(), e);
      throw new StorageException("Failed to insert record of type " + recordType.getName(), e);
    }
  }

  @Override
  public <T extends InstantRecord> T updateInstantRecord(Record containingRecord,
      RecordType<T> recordType, int id, Instant instant, Map<String, Object> attributes)
      throws StorageException {
    recordTypeRegistry.validateRecordAttributes(recordType, attributes);
    TransactionImpl currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    updateInstantRecord(currentTransaction.getConnection(),
        recordType, id, instant, attributes);
    return RecordProxy.proxyInstantRecord(recordType,
        containingRecord, id, instant, attributes);
  }

  public <T extends InstantRecord> void updateInstantRecord(Connection connection,
      RecordType<T> recordType, int id, Instant instant,
      Map<String, Object> attributes) throws StorageException {

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("UPDATE ").append(recordType.getTableName()).append(" SET ");
    Joiner.on(" = ?, ").appendTo(stringBuilder, prefixColumnNames(recordType, attributes.keySet()));
    stringBuilder.append(" = ?, ");
    stringBuilder.append(SystemColumnDefs.INSTANT.getName()).append(" = ?");
    stringBuilder.append(" WHERE ").append(SystemColumnDefs.ID_COLUMN_NAME).append(" = ?;");

    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
      int nextIndex = setParameters(recordType, stmt, 1, attributes);
      stmt.setTimestamp(nextIndex++, getTimestamp(instant));
      stmt.setInt(nextIndex, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to insert record of type " + recordType.getName(), e);
      throw new StorageException("Failed to insert record of type " + recordType.getName(), e);
    }
  }

  @Override
  public <T extends InstantRecord> void deleteInstantRecords(Record containingRecord,
      RecordType<T> recordType, ImmutableList<? extends SearchTerm> searchTerms)
      throws StorageException {
    TransactionImpl currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("DELETE FROM ").append(recordType.getTableName());
    SearchTermHandler searchTermHandler = new SearchTermHandler(recordType, searchTerms);
    searchTermHandler.appendWhereClause(stringBuilder);
    stringBuilder.append(";");

    try (PreparedStatement stmt = currentTransaction.getConnection().prepareStatement(
        stringBuilder.toString())) {
      searchTermHandler.setParameters(stmt, recordType, 1);
      stmt.executeUpdate();
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to delete records for type " + recordType.getName(), e);
      throw new StorageException("Failed to delete records for type " + recordType.getName(), e);
    }
  }

  @Override
  public <T extends InstantRecord> Iterable<T> findInstantRecords(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end, List<? extends SearchTerm> searchTerms)
      throws StorageException {
    try(ReadingDataSource readingDataSource = new ReadingDataSource()) {
      Iterable<RawInstantRecord<T>> rawRecords =
          findInstantRecords(readingDataSource.getConnection(),
              recordType, start, end,
              ImmutableList.<SearchTerm>builder()
                  .addAll(searchTerms)
                  .add(ContainerIdentifierSearchTerm.of(
                      containingRecord.getRecordType(), containingRecord.getId()))
                  .build());
      ImmutableList.Builder<T> resultList = ImmutableList.builder();
      for (RawInstantRecord<T> rawRecord : rawRecords) {
        T record = RecordProxy.proxyInstantRecord(rawRecord.getRecordType(),
            containingRecord, rawRecord.getId(), rawRecord.getInstant(),
            rawRecord.getAttributes());
        resultList.add(record);
      }
      return resultList.build();
    }
  }

  public <T extends InstantRecord> Iterable<RawInstantRecord<T>> findInstantRecords(
      Connection connection, RecordType<T> recordType, Instant start, Instant end,
      List<? extends SearchTerm> searchTerms)
      throws StorageException {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("SELECT * FROM ").append(recordType.getTableName());
    searchTerms = ImmutableList.<SearchTerm>builder()
        .addAll(searchTerms)
        .add(InstantSearchTerm.of(
            SystemColumnDefs.INSTANT,
            SearchOperator.GREATER_THAN_OR_EQUAL,
            start))
        .add(InstantSearchTerm.of(
            SystemColumnDefs.INSTANT,
            SearchOperator.LESS_THAN,
            end))
        .build();
    SearchTermHandler searchTermHandler = new SearchTermHandler(recordType, searchTerms);
    searchTermHandler.appendWhereClause(stringBuilder);
    stringBuilder.append(" ORDER BY ").append(SystemColumnDefs.INSTANT.getName()).append(" ASC ;");

    ImmutableList.Builder<RawInstantRecord<T>> records = ImmutableList.builder();
    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
      searchTermHandler.setParameters(stmt, recordType, 1);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          RecordType<T> loadedRecordType = loadedRecordType(rs, recordType);
          records.add(new RawInstantRecord<>(
              rs.getInt(SystemColumnDefs.CONTAINER_ID.getName()),
              loadedRecordType,
              rs.getInt(SystemColumnDefs.ID_COLUMN_NAME),
              getInstant(rs, SystemColumnDefs.INSTANT),
              readAttributes(loadedRecordType, rs)));
        }
      }
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to read records for type " + recordType.getName(), e);
      throw new StorageException("Failed to read records for type " + recordType.getName(), e);
    }
    return records.build();
  }

  @Override
  public <T extends IntervalRecord> T getIntervalRecord(Record containingRecord,
      RecordType<T> recordType, Instant date) throws StorageException {
    try(ReadingDataSource readingDataSource = new ReadingDataSource()) {
      RawIntervalRecord<T> rawRecord =
          getIntervalRecord(readingDataSource.getConnection(), containingRecord, recordType, date);
      if (rawRecord == null) {
        return null;
      }
      return RecordProxy.proxyIntervalRecord(rawRecord.getRecordType(),
          containingRecord, rawRecord.getId(),
          rawRecord.getStart(), rawRecord.getEnd(), rawRecord.getAttributes());
    }
  }

  @Nullable
  private <T extends IntervalRecord> RawIntervalRecord<T> getIntervalRecord(Connection connection,
      Record containingRecord, RecordType<T> recordType, Instant date) throws StorageException {

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("SELECT * FROM ").append(recordType.getTableName());
    stringBuilder.append(" WHERE ");
    stringBuilder.append(SystemColumnDefs.CONTAINER_ID.getName()).append(" = ? AND ");
    stringBuilder.append(SystemColumnDefs.START_TIME.getName()).append(" <= ? AND ");
    stringBuilder.append(SystemColumnDefs.END_TIME.getName()).append(" > ?;");

    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
      stmt.setInt(1, containingRecord.getId());
      stmt.setTimestamp(2, getTimestamp(date));
      stmt.setTimestamp(3, getTimestamp(date));
      try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          return null;
        }

        RecordType<T> loadedRecordType = loadedRecordType(rs, recordType);
        return new RawIntervalRecord<>(
            rs.getInt(SystemColumnDefs.CONTAINER_ID.getName()),
            loadedRecordType,
            rs.getInt(SystemColumnDefs.ID_COLUMN_NAME),
            getInstant(rs, SystemColumnDefs.START_TIME),
            getInstant(rs, SystemColumnDefs.END_TIME),
            readAttributes(loadedRecordType, rs));
      }
    } catch (SQLException e) {
      logger.log(Level.WARNING,
          "Failed to read interval record for type " + recordType.getName(), e);
      throw new StorageException(
          "Failed to read interval record for type " + recordType.getName(), e);
    }
  }

  @Override
  public <T extends Record> Iterable<T> findRecords(RecordType<T> recordType,
      List<? extends SearchTerm> searchTerms) throws StorageException {
    try(ReadingDataSource readingDataSource = new ReadingDataSource()) {
      // TODO(mchaston): add ordering
      Iterable<RawRecord<T>> rawRecords =
          findRecords(readingDataSource.getConnection(), recordType, searchTerms);
      ImmutableList.Builder<T> resultList = ImmutableList.builder();
      for (RawRecord<T> rawRecord : rawRecords) {
        T record = RecordProxy.proxyRecord(rawRecord.getRecordType(),
            null, rawRecord.getId(), rawRecord.getAttributes());
        resultList.add(record);
      }
      return resultList.build();
    }
  }

  private <T extends Record> Iterable<RawRecord<T>> findRecords(Connection connection,
      RecordType<T> recordType, List<? extends SearchTerm> searchTerms)
      throws StorageException {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("SELECT * FROM ").append(recordType.getTableName());
    SearchTermHandler searchTermHandler = new SearchTermHandler(recordType, searchTerms);
    searchTermHandler.appendWhereClause(stringBuilder);
    stringBuilder.append(";");

    ImmutableList.Builder<RawRecord<T>> records = ImmutableList.builder();
    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
      searchTermHandler.setParameters(stmt, recordType, 1);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          int id = rs.getInt(SystemColumnDefs.ID_COLUMN_NAME);
          RecordType<T> loadedRecordType = loadedRecordType(rs, recordType);
          records.add(new RawRecord<>(loadedRecordType, id,
              readAttributes(loadedRecordType, rs)));
        }
      }
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to read records for type " + recordType.getName(), e);
      throw new StorageException("Failed to read records for type " + recordType.getName(), e);
    }
    return records.build();
  }

  @Override
  public <T extends IntervalRecord> Iterable<T> findIntervalRecords(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end, List<? extends SearchTerm> searchTerms)
      throws StorageException {
    try (ReadingDataSource readingDataSource = new ReadingDataSource()) {
      Iterable<RawIntervalRecord<T>> rawRecords =
          findIntervalRecords(readingDataSource.getConnection(), containingRecord, recordType,
              start, end, searchTerms);
      ImmutableList.Builder<T> resultList = ImmutableList.builder();
      for (RawIntervalRecord<T> rawRecord : rawRecords) {
        T record = RecordProxy.proxyIntervalRecord(
            rawRecord.getRecordType(), containingRecord, rawRecord.getId(),
            rawRecord.getStart(), rawRecord.getEnd(), rawRecord.getAttributes());
        resultList.add(record);
      }
      return resultList.build();
    }
  }

  private <T extends IntervalRecord> Iterable<RawIntervalRecord<T>> findIntervalRecords(
      Connection connection, Record containingRecord, RecordType<T> recordType,
      Instant start, Instant end, List<? extends SearchTerm> searchTerms) throws StorageException {

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("SELECT * FROM ").append(recordType.getTableName());
    searchTerms = ImmutableList.<SearchTerm>builder()
        .addAll(searchTerms)
        .add(ContainerIdentifierSearchTerm.of(
            containingRecord.getRecordType(), containingRecord.getId()))
        .add(InstantSearchTerm.of(
            SystemColumnDefs.START_TIME,
            SearchOperator.GREATER_THAN_OR_EQUAL,
            start))
        .add(InstantSearchTerm.of(
            SystemColumnDefs.START_TIME,
            SearchOperator.LESS_THAN,
            end))
        .build();
    SearchTermHandler searchTermHandler = new SearchTermHandler(recordType, searchTerms);
    searchTermHandler.appendWhereClause(stringBuilder);
    stringBuilder.append(" ORDER BY ")
        .append(SystemColumnDefs.START_TIME.getName()).append(" ASC ;");

    try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
      searchTermHandler.setParameters(stmt, recordType, 1);
      ImmutableList.Builder<RawIntervalRecord<T>> results = ImmutableList.builder();
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          RecordType<T> loadedRecordType = loadedRecordType(rs, recordType);
          results.add(new RawIntervalRecord<>(
              rs.getInt(SystemColumnDefs.CONTAINER_ID.getName()),
              loadedRecordType,
              rs.getInt(SystemColumnDefs.ID_COLUMN_NAME),
              getInstant(rs, SystemColumnDefs.START_TIME),
              getInstant(rs, SystemColumnDefs.END_TIME),
              readAttributes(loadedRecordType, rs)));
        }
      }
      return results.build();
    } catch (SQLException e) {
      logger.log(Level.WARNING,
          "Failed to read interval records for type " + recordType.getName(), e);
      throw new StorageException(
          "Failed to read interval records for type " + recordType.getName(), e);
    }
  }

  @Override
  public <T extends InstantRecord> Report runReport(RecordType<T> recordType,
      int startYear, int endYear, ReportDateGranularity granularity,
      List<? extends SearchTerm> searchTerms,
      @Nullable String containerIdDimension, List<String> dimensions, List<String> measures)
      throws StorageException {
    ReportBuilder reportBuilder =
        new ReportBuilder(granularity, startYear, endYear, containerIdDimension, dimensions,
            measures);
    try (ReadingDataSource readingDataSource = new ReadingDataSource()) {
      // Get all records that would match.
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("SELECT ");
      // Include the date and container ID.
      stringBuilder.append(getInstantFunction(granularity))
          .append(" AS ").append(SystemColumnDefs.INSTANT.getName());
      stringBuilder.append(", ").append(SystemColumnDefs.CONTAINER_ID.getName());
      // Include the dimensions.
      for (String dimensionColumn : prefixColumnNames(recordType, dimensions)) {
        stringBuilder.append(", ").append(dimensionColumn);
      }
      // Include sums of the measures.
      for (String measureColumn : prefixColumnNames(recordType, measures)) {
        stringBuilder.append(", SUM(").append(measureColumn).append(") AS ").append(measureColumn);
      }
      stringBuilder.append(" FROM ").append(recordType.getTableName());
      searchTerms = ImmutableList.<SearchTerm>builder()
          .addAll(searchTerms)
          .add(InstantSearchTerm.of(
              SystemColumnDefs.INSTANT,
              SearchOperator.GREATER_THAN_OR_EQUAL,
              DateUtil.BEGINNING_OF_TIME))
          .add(InstantSearchTerm.of(
              SystemColumnDefs.INSTANT,
              SearchOperator.LESS_THAN,
              DateUtil.endOfYear(endYear)))
          .build();
      SearchTermHandler searchTermHandler = new SearchTermHandler(recordType, searchTerms);
      searchTermHandler.appendWhereClause(stringBuilder);
      // Group by the date  and container ID.
      stringBuilder.append(" GROUP BY ").append(SystemColumnDefs.INSTANT.getName());
      stringBuilder.append(", ").append(SystemColumnDefs.CONTAINER_ID.getName());
      // Include the dimensions.
      for (String dimensionColumn : prefixColumnNames(recordType, dimensions)) {
        stringBuilder.append(", ").append(dimensionColumn);
      }
      stringBuilder.append(";");

      ImmutableMap.Builder<String, JdbcTypeHandler> jdbcTypeHandlersBuilder =
          ImmutableMap.builder();
      for (String dimension : dimensions) {
        jdbcTypeHandlersBuilder.put(dimension, recordType.getJdbcTypeHandler(dimension));
      }
      for (String measure : measures) {
        jdbcTypeHandlersBuilder.put(measure, recordType.getJdbcTypeHandler(measure));
      }
      ImmutableMap<String, JdbcTypeHandler> jdbcTypeHandlers = jdbcTypeHandlersBuilder.build();

      try (PreparedStatement stmt =
               readingDataSource.getConnection().prepareStatement(stringBuilder.toString())) {
        stmt.setTimestamp(1, getTimestamp(DateUtil.endOfYear(startYear - 1)));
        searchTermHandler.setParameters(stmt, recordType, 2);
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            // Group and sum to create the results.
            reportBuilder.aggregateEntry(
                getInstant(rs, SystemColumnDefs.INSTANT),
                rs.getInt(SystemColumnDefs.CONTAINER_ID.getName()),
                readAttributes(jdbcTypeHandlers, rs));
          }
        }
      } catch (SQLException e) {
        logger.log(Level.WARNING, "Failed to read records for type " + recordType.getName(), e);
        throw new StorageException("Failed to read records for type " + recordType.getName(), e);
      }
    }

    return reportBuilder.build();
  }

  private String getInstantFunction(ReportDateGranularity granularity) {
    switch (granularity) {
      case YEAR:
        return "reporting_year(?, " + SystemColumnDefs.INSTANT.getName() + ")";
      case MONTH:
        return "reporting_month(?, " + SystemColumnDefs.INSTANT.getName() + ")";
      case DAY:
        return "reporting_day(?, " + SystemColumnDefs.INSTANT.getName() + ")";
      default:
        throw new UnsupportedOperationException(
            "Granularity " + granularity + " is not supported.");
    }
  }

  private Connection getNewConnection() throws StorageException {
    try {
      return dataSource.getConnection();
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to get connection", e);
      throw new StorageException("Failed to get connection", e);
    }
  }

  private void closeConnection(@Nullable Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.log(Level.WARNING, "Failed to close connection", e);
      }
    }
  }

  private Map<String, Object> readAttributes(RecordType<?> recordType, ResultSet rs)
      throws SQLException {
    return readAttributes(recordType.getJdbcTypeHandlers(), rs);
  }

  private Map<String, Object> readAttributes(Map<String, JdbcTypeHandler> jdbcTypeHandlers,
      ResultSet rs) throws SQLException {
    Map<String, Object> attributes = new HashMap<>();
    for (Map.Entry<String, JdbcTypeHandler> jdbcTypeHandlerEntry : jdbcTypeHandlers.entrySet()) {
      Object value = jdbcTypeHandlerEntry.getValue().get(rs);
      if (value != null) {
        attributes.put(jdbcTypeHandlerEntry.getKey(), value);
      }
    }
    return ImmutableMap.copyOf(attributes);
  }

  private int setParameters(RecordType recordType, PreparedStatement stmt, int startIndex,
      Map<String, Object> attributes) throws SQLException {
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      JdbcTypeHandler jdbcTypeHandler = recordType.getJdbcTypeHandler(entry.getKey());
      jdbcTypeHandler.set(stmt, startIndex++, entry.getValue());
    }
    return startIndex;
  }

  private void appendQuestionMarks(StringBuilder stringBuilder, int numberOfQuestionMarks) {
    for (int i = 0; i < numberOfQuestionMarks; i++) {
      if (i > 0) {
        stringBuilder.append(", ");
      }
      stringBuilder.append('?');
    }
  }

  private static Instant getInstant(ResultSet rs, ColumnDef columnDef) throws SQLException {
    return new Instant(rs.getTimestamp(columnDef.getName()));
  }

  private static Timestamp getTimestamp(Instant instant) {
    return new Timestamp(instant.getMillis());
  }

  private static String prefixColumnName(RecordType<?> recordType, String attributeName) {
    return recordType.getJdbcTypeHandler(attributeName).getColumnName();
  }

  private static Iterable<String> prefixColumnNames(
      final RecordType<?> recordType, Iterable<String> attributeNames) {
    return Iterables.transform(attributeNames, new Function<String, String>() {
      @Override
      public String apply(String attributeName) {
        return prefixColumnName(recordType, attributeName);
      }
    });
  }

  private static void appendAttributeColumnNames(StringBuilder stringBuilder,
      RecordType<?> recordType, Map<String, Object> attributes) {
    Joiner.on(", ").appendTo(stringBuilder, prefixColumnNames(recordType, attributes.keySet()));
  }

  private class ReadingDataSource implements AutoCloseable {

    private Connection localConnection;

    Connection getConnection() throws StorageException {
      if (localConnection == null) {
        TransactionImpl currentTransaction = CURRENT_TRANSACTION.get();
        if (currentTransaction != null) {
          return currentTransaction.getConnection();
        }
        localConnection = getNewConnection();
      }
      return localConnection;
    }

    @Override
    public void close() {
      if (localConnection != null) {
        closeConnection(localConnection);
        localConnection = null;
      }
    }
  }

  private class SearchTermHandler {
    private final RecordType<?> recordType;
    private final List<? extends SearchTerm> searchTerms;
    // This is linked to ensure that they are set in the same order as they need to be written.
    private final List<ParameterValue> parameterValues = new LinkedList<>();

    public SearchTermHandler(RecordType<?> recordType, List<? extends SearchTerm> searchTerms) {
      this.recordType = recordType;
      this.searchTerms = searchTerms;
    }

    public void appendWhereClause(StringBuilder stringBuilder) {
      if (searchTerms.isEmpty()) {
        return;
      }
      stringBuilder.append(" WHERE ");
      writeAndClause(stringBuilder, searchTerms);
    }

    private void writeAndClause(StringBuilder stringBuilder,
        List<? extends SearchTerm> searchTerms) {
      writeJoinedClause(stringBuilder, searchTerms, "AND");
    }

    private void writeOrClause(StringBuilder stringBuilder,
        List<? extends SearchTerm> searchTerms) {
      writeJoinedClause(stringBuilder, searchTerms, "OR");
    }

    private void writeJoinedClause(StringBuilder stringBuilder,
        List<? extends SearchTerm> searchTerms, String joiningTerm) {
      boolean first = true;
      stringBuilder.append("(");
      for (SearchTerm searchTerm : searchTerms) {
        if (first) {
          first = false;
        } else {
          stringBuilder.append(" ").append(joiningTerm).append(" ");
        }
        writeSearchTerm(stringBuilder, searchTerm);
      }
      stringBuilder.append(")");
    }

    private void writeSearchTerm(StringBuilder stringBuilder, SearchTerm searchTerm) {
      if (searchTerm instanceof OrSearchTerm) {
        writeOrClause(stringBuilder, ((OrSearchTerm) searchTerm).getSearchTerms());
      } else if (searchTerm instanceof ContainerIdentifierSearchTerm) {
        ContainerIdentifierSearchTerm containerIdentifierSearchTerm =
            (ContainerIdentifierSearchTerm) searchTerm;
        stringBuilder.append(SystemColumnDefs.CONTAINER_ID.getName()).append(" = ?");
        parameterValues.add(new IntegerParameterValue(containerIdentifierSearchTerm.getId()));
      } else if (searchTerm instanceof IdentifierSearchTerm) {
        IdentifierSearchTerm identifierSearchTerm = (IdentifierSearchTerm) searchTerm;
        stringBuilder.append(SystemColumnDefs.ID_COLUMN_NAME).append(" = ?");
        parameterValues.add(new IntegerParameterValue(identifierSearchTerm.getId()));
      } else if (searchTerm instanceof AttributeSearchTerm) {
        AttributeSearchTerm attributeSearchTerm = (AttributeSearchTerm) searchTerm;
        stringBuilder.append(prefixColumnName(recordType, attributeSearchTerm.getAttribute()))
            .append(" ")
            .append(attributeSearchTerm.getOperator().toSql()).append(" ?");
        parameterValues.add(new AttributeParameterValue(
            attributeSearchTerm.getAttribute(), attributeSearchTerm.getValue()));
      } else if (searchTerm instanceof InstantSearchTerm) {
        InstantSearchTerm instantSearchTerm = (InstantSearchTerm) searchTerm;
        stringBuilder.append(instantSearchTerm.getColumnDef().getName()).append(" ")
            .append(instantSearchTerm.getOperator().toSql()).append(" ?");
        parameterValues.add(new InstantParameterValue(instantSearchTerm.getInstant()));
      } else {
        throw new UnsupportedOperationException(
            "Search term " + searchTerm.getClass().getName() + " is not supported.");
      }
    }

    public <T extends Record> void setParameters(PreparedStatement stmt, RecordType<T> recordType,
        int index) throws SQLException {
      for (ParameterValue parameterValue : parameterValues) {
        parameterValue.set(stmt, index++, recordType);
      }
    }

    private abstract class ParameterValue {
      abstract <T extends Record> void set(
          PreparedStatement stmt, int index, RecordType<T> recordType) throws SQLException;
    }

    private class IntegerParameterValue extends ParameterValue {
      private final int value;
      IntegerParameterValue(int value) {
        this.value = value;
      }

      @Override
      <T extends Record> void set(PreparedStatement stmt, int index, RecordType<T> recordType)
          throws SQLException {
        stmt.setInt(index, value);
      }
    }

    private class AttributeParameterValue extends ParameterValue {
      private final String attribute;
      private final Object value;

      AttributeParameterValue(String attribute, Object value) {
        this.attribute = attribute;
        this.value = value;
      }

      @Override
      <T extends Record> void set(PreparedStatement stmt, int index, RecordType<T> recordType)
          throws SQLException {
        recordType.getJdbcTypeHandler(attribute).set(stmt, index, value);
      }
    }

    private class InstantParameterValue extends ParameterValue {
      private final Instant instant;

      InstantParameterValue(Instant instant) {
        this.instant = instant;
      }

      @Override
      <T extends Record> void set(PreparedStatement stmt, int index, RecordType<T> recordType)
          throws SQLException {
        stmt.setTimestamp(index, getTimestamp(instant));
      }
    }
  }
}
