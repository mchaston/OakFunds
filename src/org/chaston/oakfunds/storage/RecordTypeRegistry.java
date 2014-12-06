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
public interface RecordTypeRegistry {
  void validateRecordAttributes(RecordType<?> recordType, Map<String, Object> attributes,
      boolean create) throws StorageException;

  <T extends Record> RecordType<T> getType(String name, RecordType<T> recordType);

  Iterable<RecordType> getAssignableTypes(RecordType recordType);
}
