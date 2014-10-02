package org.chaston.oakfunds.storage;

import org.joda.time.Instant;

/**
* TODO(mchaston): write JavaDocs
*/
class InstantRecordKey implements Comparable<InstantRecordKey> {
  private final Instant instant;
  private final int id;

  InstantRecordKey(Instant instant, int id) {
    this.instant = instant;
    this.id = id;
  }

  public Instant getInstant() {
    return instant;
  }

  public int getId() {
    return id;
  }

  @Override
  public int compareTo(InstantRecordKey that) {
    int result = this.instant.compareTo(that.instant);
    if (result == 0) {
      result =  this.id - that.id;
    }
    return result;
  }
}
