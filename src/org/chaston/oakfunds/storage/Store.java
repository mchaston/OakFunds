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

import com.google.common.collect.ImmutableList;
import org.joda.time.Instant;

import java.util.List;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface Store {

  void registerType(RecordType<?> recordType);

  Transaction startTransaction() throws StorageException;

  <T extends Record> T createRecord(RecordType<T> recordType, int id,
      Map<String, Object> attributes) throws StorageException;

  <T extends Record> T createRecord(RecordType<T> recordType,
      Map<String, Object> attributes) throws StorageException;

  <T extends Record> T getRecord(RecordType<T> recordType, int id) throws StorageException;

  <T extends Record> T updateRecord(T record, Map<String, Object> attributes) throws StorageException;

  <T extends IntervalRecord> T updateIntervalRecord(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end,
      Map<String, Object> attributes)
      throws StorageException;

  <T extends InstantRecord> T insertInstantRecord(Record containingRecord,
      RecordType<T> recordType, Instant instant, Map<String, Object> attributes)
      throws StorageException;

  <T extends InstantRecord> T updateInstantRecord(Record containingRecord,
      RecordType<T> recordType, int id, Instant instant, Map<String, Object> attributes)
      throws StorageException;

  <T extends InstantRecord> void deleteInstantRecords(Record containingRecord,
      RecordType<T> recordType, ImmutableList<? extends SearchTerm> searchTerms)
      throws StorageException;

  <T extends InstantRecord> Iterable<T> findInstantRecords(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end,
      List<? extends SearchTerm> searchTerms) throws StorageException;

  <T extends IntervalRecord> T getIntervalRecord(Record containingRecord,
      RecordType<T> recordType, Instant date) throws StorageException;

  <T extends Record> Iterable<T> findRecords(RecordType<T> recordType,
      List<? extends SearchTerm> searchTerms) throws StorageException;

  <T extends IntervalRecord> Iterable<T> findIntervalRecords(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end, List<? extends SearchTerm> searchTerms)
      throws StorageException;

  <T extends InstantRecord> Report runReport(RecordType<T> type,
      int startYear, int endYear, ReportDateGranularity granularity,
      List<? extends SearchTerm> searchTerms, List<String> dimensions, List<String> measures);
}
