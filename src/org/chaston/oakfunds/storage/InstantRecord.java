package org.chaston.oakfunds.storage;

import org.joda.time.Instant;

/**
 * TODO(mchaston): write JavaDocs
 */
public class InstantRecord extends Record {
  private final Instant instant;

  protected InstantRecord(RecordType recordType, int id, Instant instant) {
    super(recordType, id);
    this.instant = instant;
  }

  public Instant getInstant() {
    return instant;
  }
}
