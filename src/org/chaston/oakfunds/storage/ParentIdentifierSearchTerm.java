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

import javax.annotation.Nullable;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ParentIdentifierSearchTerm extends SearchTerm {
  private final RecordType<?> recordType;
  private final int id;

  private ParentIdentifierSearchTerm(RecordType<?> recordType, int id) {
    this.recordType = recordType;
    this.id = id;
  }

  public static ParentIdentifierSearchTerm of(RecordType<?> recordType, int id) {
    return new ParentIdentifierSearchTerm(recordType, id);
  }

  public RecordType<?> getRecordType() {
    return recordType;
  }

  public int getId() {
    return id;
  }

  @Override
  boolean matches(@Nullable Integer parentId, int id, Map<String, Object> attributes) {
    if (parentId == null) {
      return false;
    }
    return this.id == parentId;
  }
}
