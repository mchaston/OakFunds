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

import org.joda.time.Instant;

/**
 * TODO(mchaston): write JavaDocs
 */
class IntervalRecordKey implements Comparable<IntervalRecordKey> {
  private final RecordType recordType;
  private final int id;
  private final Instant start;
  private Instant end;

  IntervalRecordKey(RecordType recordType, int id, Instant start, Instant end) {
    this.recordType = recordType;
    this.id = id;
    this.start = start;
    this.end = end;
  }

  public RecordType getRecordType() {
    return recordType;
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
      result = this.id - that.id;
    }
    return result;
  }

  public void setEnd(Instant end) {
    this.end = end;
  }
}
