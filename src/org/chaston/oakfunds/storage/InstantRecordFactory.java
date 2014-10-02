package org.chaston.oakfunds.storage;

import org.joda.time.Instant;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface InstantRecordFactory<T extends InstantRecord> {
  T newInstance(int id, Instant interval);

  RecordType getRecordType();
}
