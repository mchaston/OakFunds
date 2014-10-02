package org.chaston.oakfunds.storage;

import org.joda.time.Instant;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface IntervalRecordFactory<T extends IntervalRecord> {
  T newInstance(int id, Instant start, Instant end);

  RecordType getRecordType();
}
