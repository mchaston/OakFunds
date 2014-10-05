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
class InstantRecordKey implements Comparable<InstantRecordKey> {
  private final RecordType recordType;
  private final Instant instant;
  private final int id;

  InstantRecordKey(RecordType recordType, Instant instant, int id) {
    this.recordType = recordType;
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
      result = this.id - that.id;
    }
    return result;
  }

  public RecordType getRecordType() {
    return recordType;
  }
}
