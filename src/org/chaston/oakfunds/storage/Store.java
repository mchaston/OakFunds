package org.chaston.oakfunds.storage;

import org.joda.time.Instant;

import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface Store {
  Transaction startTransaction() throws StorageException;

  <T extends Record> T createRecord(RecordFactory<T> recordFactory, int id,
      Map<String, Object> attributes) throws StorageException;

  <T extends Record> T createRecord(RecordFactory<T> recordFactory,
      Map<String, Object> attributes) throws StorageException;

  <T extends Record> T getRecord(RecordFactory<T> recordFactory, int id) throws StorageException;

  <T extends IntervalRecord> void updateIntervalRecord(Record containingRecord,
      IntervalRecordFactory<T> recordFactory, Instant start, Instant end, Map<String, Object> attributes)
      throws StorageException;

  <T extends InstantRecord> int insertInstantRecord(Record containingRecord,
      InstantRecordFactory<T> recordFactory, Instant instant, Map<String, Object> attributes)
      throws StorageException;

  <T extends InstantRecord> Iterable<T> getInstantRecords(Record containingRecord,
      InstantRecordFactory<T> recordFactory, Instant start, Instant end) throws StorageException;

  <T extends IntervalRecord> T getIntervalRecord(Record containingRecord,
      IntervalRecordFactory<T> recordFactory, Instant date) throws StorageException;
}
