package org.chaston.oakfunds.storage;

import org.joda.time.Instant;

/**
* TODO(mchaston): write JavaDocs
*/
class IntervalRecordKey implements Comparable<IntervalRecordKey> {
  private final int id;
  private final Instant start;
  private Instant end;

  IntervalRecordKey(int id, Instant start, Instant end) {
    this.id = id;
    this.start = start;
    this.end = end;
  }

  public int getId() {
    return id;
  }

  public Instant getStart() {
    return start;
  }

  public Instant getEnd() {
    return end;
  }

  @Override
  public int compareTo(IntervalRecordKey that) {
    int result = this.start.compareTo(that.start);
    if (result == 0) {
      result =  this.id - that.id;
    }
    return result;
  }

  public void setEnd(Instant end) {
    this.end = end;
  }
}
