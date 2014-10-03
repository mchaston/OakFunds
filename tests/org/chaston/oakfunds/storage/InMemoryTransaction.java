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

import com.google.common.collect.ImmutableMap;
import org.joda.time.Instant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO(mchaston): write JavaDocs
 */
class InMemoryTransaction implements Transaction, RecordInserter {

  private static final Map<RecordType, AtomicInteger> TYPE_COUNTERS = new HashMap<>();

  private final Map<RecordType, Map<Integer, InMemoryRecord>> tables = new HashMap<>();
  private final InMemoryStore store;

  InMemoryTransaction(InMemoryStore store) {
    this.store = store;
  }

  @Override
  public void commit() throws StorageException {
    store.commit(tables);
    store.endTransaction();
  }

  @Override
  public void rollback() {
    tables.clear();
    store.endTransaction();
  }

  @Override
  public int insertRecord(RecordType recordType, Map<String, Object> attributes)
      throws StorageException {
    int id = getNextId(recordType);
    addRecordToTransaction(recordType, id, new InMemoryRecord(attributes));
    return id;
  }

  @Override
  public void insertRecord(RecordType recordType, int id, Map<String, Object> attributes)
      throws StorageException {
    addRecordToTransaction(recordType, id, new InMemoryRecord(attributes));
  }

  @Override
  public int insertInstantRecord(RecordType recordType, Instant date,
      Map<String, Object> attributes) throws StorageException {
    return getNextId(recordType);
  }

  @Override
  public int insertIntervalRecord(RecordType recordType, Instant start, Instant end,
      Map<String, Object> attributes) throws StorageException {
    return getNextId(recordType);
  }

  void addRecordToTransaction(RecordType recordType, int id, InMemoryRecord inMemoryRecord) {
    getTable(recordType).put(id, inMemoryRecord);
  }

  InMemoryRecord getRecord(RecordType recordType, int id) {
    return getTable(recordType).get(id);
  }

  void updateIntervalRecord(InMemoryRecord inMemoryRecord, RecordType recordType,
      Instant start, Instant end, Map<String, Object> attributes) throws StorageException {
    inMemoryRecord.updateIntervalRecord(this, recordType, start, end, attributes);
  }

  int insertInstantRecord(InMemoryRecord inMemoryRecord, RecordType recordType,
      Instant instant, Map<String, Object> attributes) throws StorageException {
    return inMemoryRecord.insertInstantRecord(this, recordType, instant, attributes);
  }

  boolean containsRecord(RecordType recordType, int id) {
    return getTable(recordType).containsKey(id);
  }

  Map<Integer, InMemoryRecord> findRecords(RecordType recordType,
      List<SearchTerm> searchTerms) {
    ImmutableMap.Builder<Integer, InMemoryRecord> results = ImmutableMap.builder();
    for (Map.Entry<Integer, InMemoryRecord> entry : getTable(recordType).entrySet()) {
      InMemoryRecord record = entry.getValue();
      if (record.matchesSearchTerms(searchTerms)) {
        results.put(entry);
      }
    }
    return results.build();
  }

  private int getNextId(RecordType recordType) {
    AtomicInteger counter = TYPE_COUNTERS.get(recordType.getRootType());
    if (counter == null) {
      counter = new AtomicInteger(1000);
      TYPE_COUNTERS.put(recordType, counter);
    }
    return counter.getAndIncrement();
  }

  private Map<Integer, InMemoryRecord> getTable(RecordType recordType) {
    Map<Integer, InMemoryRecord> table = tables.get(recordType);
    if (table == null) {
      table = new HashMap<>();
      tables.put(recordType, table);
    }
    return table;
  }
}
