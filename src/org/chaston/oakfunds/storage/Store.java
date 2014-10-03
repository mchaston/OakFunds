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

import java.util.List;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface Store {
  Transaction startTransaction() throws StorageException;

  <T extends Record> T createRecord(RecordFactory<T> recordFactory, int id,
      Map<String, Object> attributes) throws StorageException;

  <T extends Record> T createRecord(RecordFactory<T> recordFactory,
      Map<String, Object> attributes) throws StorageException;

  <T extends Record> T getRecord(RecordFactory<T> recordFactory, int id) throws StorageException;

  <T extends IntervalRecord> void updateIntervalRecord(Record containingRecord,
      IntervalRecordFactory<T> recordFactory, Instant start, Instant end,
      Map<String, Object> attributes)
      throws StorageException;

  <T extends InstantRecord> int insertInstantRecord(Record containingRecord,
      InstantRecordFactory<T> recordFactory, Instant instant, Map<String, Object> attributes)
      throws StorageException;

  <T extends InstantRecord> Iterable<T> getInstantRecords(Record containingRecord,
      InstantRecordFactory<T> recordFactory, Instant start, Instant end) throws StorageException;

  <T extends IntervalRecord> T getIntervalRecord(Record containingRecord,
      IntervalRecordFactory<T> recordFactory, Instant date) throws StorageException;

  <T extends Record> List<T> findRecords(RecordFactory<T> recordFactory,
      List<SearchTerm> searchTerms) throws StorageException;
}
