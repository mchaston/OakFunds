package org.chaston.oakfunds.storage;

import org.joda.time.Instant;

import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
interface RecordInserter {
  int insertRecord(RecordType recordType,
      Map<String, Object> attributes) throws StorageException;

  void insertRecord(RecordType recordType, int id,
      Map<String, Object> attributes) throws StorageException;

  int insertInstantRecord(RecordType recordType, Instant date,
      Map<String, Object> attributes) throws StorageException;

  int insertIntervalRecord(RecordType recordType, Instant start, Instant end,
      Map<String, Object> attributes) throws StorageException;
}
