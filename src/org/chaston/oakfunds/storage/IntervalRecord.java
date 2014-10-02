package org.chaston.oakfunds.storage;

import org.joda.time.Instant;

/**
 * TODO(mchaston): write JavaDocs
 */
public class IntervalRecord extends Record {
  private final Instant start;
  private final Instant end;

  protected IntervalRecord(RecordType recordType, int id, Instant start, Instant end) {
    super(recordType, id);
    this.start = start;
    this.end = end;
  }

  public Instant getStart() {
    return start;
  }

  public Instant getEnd() {
    return end;
  }
}
