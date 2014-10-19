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

import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class RawIntervalRecord<T extends IntervalRecord> {
  private final int containerId;
  private final RecordType<T> recordType;
  private final int id;
  private final Instant start;
  private final Instant end;
  private final Map<String, Object> attributes;

  public RawIntervalRecord(int containerId, RecordType<T> recordType, int id, Instant start, Instant end,
      Map<String, Object> attributes) {
    this.containerId = containerId;
    this.recordType = recordType;
    this.id = id;
    this.start = start;
    this.end = end;
    this.attributes = attributes;
  }

  public int getContainerId() {
    return containerId;
  }

  public RecordType<T> getRecordType() {
    return recordType;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
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
}
