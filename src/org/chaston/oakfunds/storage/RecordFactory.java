package org.chaston.oakfunds.storage;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface RecordFactory<T extends Record> {
  T newInstance(int id);

  RecordType getRecordType();
}
