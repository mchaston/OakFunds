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

import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class RawRecord<T extends Record> {
  private final RecordType<T> recordType;
  private final int id;
  private final Map<String, Object> attributes;

  public RawRecord(RecordType<T> recordType, int id, Map<String, Object> attributes) {
    this.recordType = recordType;
    this.id = id;
    this.attributes = attributes;
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
}
