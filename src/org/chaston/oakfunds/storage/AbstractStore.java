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

import java.util.HashMap;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
abstract class AbstractStore implements Store {

  private final Map<String, RecordFactory<?>> recordFactories = new HashMap<>();
  private final Map<String, InstantRecordFactory<?>> instantRecordFactories = new HashMap<>();
  private final Map<String, IntervalRecordFactory<?>> intervalRecordFactories = new HashMap<>();

  @Override
  public <T extends Record> void registerType(RecordType<T> recordType,
      RecordFactory<? extends T> recordFactory) {
    if (!Record.class.isAssignableFrom( recordType.getRecordTypeClass())) {
      throw new IllegalArgumentException("RecordType " + recordType + " does not represent a normal record type.");
    }
    if (recordFactories.containsKey(recordType.getName())) {
      throw new IllegalStateException(
          "A RecordType named " + recordType.getName() + " is already registered.");
    }
    recordFactories.put(recordType.getName(), recordFactory);
  }

  protected <T extends Record> RecordFactory<T> getRecordFactory(RecordType<T> recordType) {
    if (!Record.class.isAssignableFrom(recordType.getRecordTypeClass())) {
      throw new IllegalArgumentException("RecordType " + recordType + " not represent a normal record type.");
    }
    RecordFactory<T> recordFactory = (RecordFactory<T>) recordFactories.get(recordType.getName());
    if (recordFactory == null) {
      throw new IllegalStateException(
          "A RecordType named " + recordType.getName() + " has not been registered.");
    }
    return recordFactory;
  }

  @Override
  public <T extends InstantRecord> void registerType(RecordType<T> recordType,
      InstantRecordFactory<? extends T> recordFactory) {
    if (!InstantRecord.class.isAssignableFrom(recordType.getRecordTypeClass())) {
      throw new IllegalArgumentException("RecordType " + recordType + " not represent an instant record type.");
    }
    if (instantRecordFactories.containsKey(recordType.getName())) {
      throw new IllegalStateException(
          "An RecordType named " + recordType.getName() + " is already registered.");
    }
    instantRecordFactories.put(recordType.getName(), recordFactory);
  }

  protected <T extends InstantRecord> InstantRecordFactory<T> getInstantRecordFactory(RecordType<T> recordType) {
    if (!InstantRecord.class.isAssignableFrom(recordType.getRecordTypeClass())) {
      throw new IllegalArgumentException("RecordType " + recordType + " does not represent a instant record type.");
    }
    InstantRecordFactory<T> recordFactory = (InstantRecordFactory<T>) instantRecordFactories.get(recordType.getName());
    if (recordFactory == null) {
      throw new IllegalStateException(
          "A RecordType named " + recordType.getName() + " has not been registered.");
    }
    return recordFactory;
  }

  @Override
  public <T extends IntervalRecord> void registerType(RecordType<T> recordType,
      IntervalRecordFactory<? extends T> recordFactory) {
    if (!IntervalRecord.class.isAssignableFrom(recordType.getRecordTypeClass())) {
      throw new IllegalArgumentException("RecordType " + recordType + " does not represent an interval record type.");
    }
    if (intervalRecordFactories.containsKey(recordType.getName())) {
      throw new IllegalStateException(
          "A RecordType named " + recordType.getName() + " is already registered.");
    }
    intervalRecordFactories.put(recordType.getName(), recordFactory);
  }

  protected <T extends IntervalRecord> IntervalRecordFactory<T> getIntervalRecordFactory(RecordType<T> recordType) {
    if (!IntervalRecord.class.isAssignableFrom(recordType.getRecordTypeClass())) {
      throw new IllegalArgumentException("RecordType " + recordType + " does not represent an interval record type.");
    }
    IntervalRecordFactory<T> recordFactory =
        (IntervalRecordFactory<T>) intervalRecordFactories.get(recordType.getName());
    if (recordFactory == null) {
      throw new IllegalStateException(
          "A RecordType named " + recordType.getName() + " has not been registered.");
    }
    return recordFactory;
  }
}
