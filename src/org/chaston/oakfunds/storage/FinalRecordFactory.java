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

/**
 * TODO(mchaston): write JavaDocs
 */
public abstract class FinalRecordFactory<T extends Record>
    implements RecordFactory<T> {
  private final RecordType<T> recordType;

  public FinalRecordFactory(RecordType<T> recordType) {
    this.recordType = recordType;
  }

  @Override
  public T newInstance(RecordType recordType, int id) {
    if (this.recordType == recordType) {
      return newInstance(id);
    }
    throw new IllegalArgumentException(
        "RecordType " + recordType + " is not supported by the "
            + this.recordType.getName() + " record factory.");
  }

  protected abstract T newInstance(int id);
}
