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
import com.google.common.collect.ImmutableMap;
import org.chaston.oakfunds.util.Pair;
import org.joda.time.Instant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class InMemoryStore extends AbstractStore {

  public static final ThreadLocal<InMemoryTransaction> CURRENT_TRANSACTION = new ThreadLocal<>();

  private final Map<RecordType, Map<Integer, InMemoryRecord>> tables = new HashMap<>();

  /**
   * Stops being externally instantiated.
   */
  InMemoryStore() {
  }

  @Override
  public Transaction startTransaction() throws StorageException {
    if (CURRENT_TRANSACTION.get() != null) {
      throw new IllegalStateException("Already in transaction.");
    }
    InMemoryTransaction newTransaction = new InMemoryTransaction(this);
    CURRENT_TRANSACTION.set(newTransaction);
    return newTransaction;
  }

  void endTransaction() {
    CURRENT_TRANSACTION.remove();
  }

  @Override
  public <T extends Record> T createRecord(RecordType<T> recordType, int id,
      Map<String, Object> attributes) throws StorageException {
    validateRecordAttributes(recordType, attributes);
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    currentTransaction.insertRecord(recordType, id, attributes);
    return getRecord(recordType, id);
  }

  @Override
  public <T extends Record> T createRecord(RecordType<T> recordType,
      Map<String, Object> attributes) throws StorageException {
    validateRecordAttributes(recordType, attributes);
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    int newId = currentTransaction.insertRecord(recordType, attributes);
    return getRecord(recordType, newId);
  }

  @Override
  public <T extends Record> T getRecord(RecordType<T> recordType, int id)
      throws StorageException {
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    InMemoryRecord inMemoryRecord = null;
    if (currentTransaction != null) {
      inMemoryRecord = currentTransaction.getRecord(recordType, id);
    }
    if (inMemoryRecord == null) {
      inMemoryRecord = getTable(recordType).get(id);
    }
    if (inMemoryRecord == null) {
      throw new StorageException(
          String.format("Record of type %s and ID %s was not found.", recordType, id));
    }
    return RecordProxy.proxyRecord((RecordType<T>) inMemoryRecord.getRecordType(),
        null, id, inMemoryRecord.getAttributes());
  }

  @Override
  public <T extends Record> T updateRecord(T record, Map<String, Object> attributes)
      throws StorageException {
    validateRecordAttributes(record.getRecordType(), attributes);
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    InMemoryRecord inMemoryRecord =
        currentTransaction.getRecord(record.getRecordType(), record.getId());
    if (inMemoryRecord == null) {
      inMemoryRecord = getTable(record.getRecordType()).get(record.getId());
      if (inMemoryRecord == null) {
        throw new StorageException(
            String.format("Record of type %s and ID %s was not found.",
                record.getRecordType(), record.getId()));
      }
      // Promote the stored value to the transaction.
      // This would be overkill for the real storage system, but is easy here.
      currentTransaction.addRecordToTransaction(
          record.getRecordType(), record.getId(), inMemoryRecord);
    }
    inMemoryRecord.updateRecord(attributes);
    return RecordProxy.proxyRecord((RecordType<T>) record.getRecordType(),
        null, record.getId(), inMemoryRecord.getAttributes());
  }

  @Override
  public <T extends Record> Iterable<T> findRecords(RecordType<T> recordType,
      List<? extends SearchTerm> searchTerms) throws StorageException {
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    Map<Integer, InMemoryRecord> results = ImmutableMap.of();
    if (currentTransaction != null) {
      results = currentTransaction.findRecords(recordType, searchTerms);
    }
    results = new HashMap<>(results);
    for (Map.Entry<Integer, InMemoryRecord> entry : getTable(recordType).entrySet()) {
      int id = entry.getKey();
      InMemoryRecord record = entry.getValue();
      if (!results.containsKey(id)
          && (currentTransaction == null
              || !currentTransaction.containsRecord(recordType, id))
          && record.matchesSearchTerms(searchTerms)) {
        results.put(id, record);
      }
    }
    // TODO(mchaston): add ordering
    ImmutableList.Builder<T> resultList = ImmutableList.builder();
    for (Map.Entry<Integer, InMemoryRecord> entry : results.entrySet()) {
      InMemoryRecord inMemoryRecord = entry.getValue();
      T record = RecordProxy.proxyRecord((RecordType<T>) inMemoryRecord.getRecordType(),
          null, entry.getKey(), entry.getValue().getAttributes());
      resultList.add(record);
    }
    return resultList.build();
  }

  @Override
  public <T extends IntervalRecord> T updateIntervalRecord(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end, Map<String, Object> attributes)
      throws StorageException {
    validateRecordAttributes(recordType, attributes);
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    InMemoryRecord inMemoryRecord =
        currentTransaction.getRecord(containingRecord.getRecordType(), containingRecord.getId());
    if (inMemoryRecord == null) {
      inMemoryRecord = getTable(containingRecord.getRecordType()).get(containingRecord.getId());
      if (inMemoryRecord == null) {
        throw new StorageException(
            String.format("Record of type %s and ID %s was not found.",
                containingRecord.getRecordType(), containingRecord.getId()));
      }
      // Promote the stored value to the transaction.
      // This would be overkill for the real storage system, but is easy here.
      currentTransaction.addRecordToTransaction(
          containingRecord.getRecordType(), containingRecord.getId(), inMemoryRecord);
    }
    int id = currentTransaction.updateIntervalRecord(
        inMemoryRecord, recordType, start, end, attributes);
    return RecordProxy.proxyIntervalRecord(recordType,
        containingRecord, id, start, end, attributes);
  }

  @Override
  public <T extends IntervalRecord> T getIntervalRecord(Record containingRecord,
      RecordType<T> recordType, Instant date) throws StorageException {
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    InMemoryRecord inMemoryRecord = null;
    if (currentTransaction != null) {
      inMemoryRecord = currentTransaction.getRecord(
          containingRecord.getRecordType(), containingRecord.getId());
    }
    if (inMemoryRecord == null) {
      inMemoryRecord = getTable(containingRecord.getRecordType()).get(containingRecord.getId());
    }
    if (inMemoryRecord == null) {
      throw new StorageException(
          String.format("Record of type %s and ID %s was not found.",
              containingRecord.getRecordType(), containingRecord.getId()));
    }
    Pair<IntervalRecordKey, Map<String, Object>> rawRecord =
        inMemoryRecord.getIntervalRecord(recordType, date);
    IntervalRecordKey intervalRecordKey = rawRecord.getFirst();
    return RecordProxy.proxyIntervalRecord((RecordType<T>) intervalRecordKey.getRecordType(),
        containingRecord, intervalRecordKey.getId(),
        intervalRecordKey.getStart(), intervalRecordKey.getEnd(),
        rawRecord.getSecond());
  }

  @Override
  public <T extends IntervalRecord> Iterable<T> findIntervalRecords(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end, List<? extends SearchTerm> searchTerms)
      throws StorageException {
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    InMemoryRecord inMemoryRecord = null;
    if (currentTransaction != null) {
      inMemoryRecord = currentTransaction.getRecord(
          containingRecord.getRecordType(), containingRecord.getId());
    }
    if (inMemoryRecord == null) {
      inMemoryRecord = getTable(containingRecord.getRecordType()).get(containingRecord.getId());
    }
    if (inMemoryRecord == null) {
      throw new StorageException(
          String.format("Record of type %s and ID %s was not found.",
              containingRecord.getRecordType(), containingRecord.getId()));
    }
    Iterable<Pair<IntervalRecordKey, Map<String, Object>>> rawRecords =
        inMemoryRecord.getIntervalRecords(recordType, start, end);
    ImmutableList.Builder<T> resultList = ImmutableList.builder();
    for (Pair<IntervalRecordKey, Map<String, Object>> rawRecord : rawRecords) {
      IntervalRecordKey intervalRecordKey = rawRecord.getFirst();
      if (matchesSearchTerms(intervalRecordKey.getId(), rawRecord.getSecond(), searchTerms)) {
        T record = RecordProxy.proxyIntervalRecord(
            (RecordType<T>) intervalRecordKey.getRecordType(),
            containingRecord, intervalRecordKey.getId(),
            intervalRecordKey.getStart(), intervalRecordKey.getEnd(),
            rawRecord.getSecond());
        resultList.add(record);
      }
    }
    return resultList.build();
  }

  @Override
  public <T extends InstantRecord> T insertInstantRecord(Record containingRecord,
      RecordType<T> recordType, Instant instant, Map<String, Object> attributes)
      throws StorageException {
    validateRecordAttributes(recordType, attributes);
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    InMemoryRecord inMemoryRecord =
        currentTransaction.getRecord(containingRecord.getRecordType(), containingRecord.getId());
    if (inMemoryRecord == null) {
      inMemoryRecord = getTable(containingRecord.getRecordType()).get(containingRecord.getId());
      if (inMemoryRecord == null) {
        throw new StorageException(
            String.format("Record of type %s and ID %s was not found.",
                containingRecord.getRecordType(), containingRecord.getId()));
      }
      // Promote the stored value to the transaction.
      // This would be overkill for the real storage system, but is easy here.
      currentTransaction.addRecordToTransaction(
          containingRecord.getRecordType(), containingRecord.getId(), inMemoryRecord);
    }
    int id = currentTransaction.insertInstantRecord(
        inMemoryRecord, recordType, instant, attributes);
    return RecordProxy.proxyInstantRecord(recordType,
        containingRecord, id, instant, attributes);
  }

  @Override
  public <T extends InstantRecord> T updateInstantRecord(Record containingRecord,
      RecordType<T> recordType, int id, Instant instant, Map<String, Object> attributes)
      throws StorageException {
    validateRecordAttributes(recordType, attributes);
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    InMemoryRecord inMemoryRecord =
        currentTransaction.getRecord(containingRecord.getRecordType(), containingRecord.getId());
    if (inMemoryRecord == null) {
      inMemoryRecord = getTable(containingRecord.getRecordType()).get(containingRecord.getId());
      if (inMemoryRecord == null) {
        throw new StorageException(
            String.format("Record of type %s and ID %s was not found.",
                containingRecord.getRecordType(), containingRecord.getId()));
      }
      // Promote the stored value to the transaction.
      // This would be overkill for the real storage system, but is easy here.
      currentTransaction.addRecordToTransaction(
          containingRecord.getRecordType(), containingRecord.getId(), inMemoryRecord);
    }
    currentTransaction.updateInstantRecord(
        inMemoryRecord, recordType, id, instant, attributes);
    return RecordProxy.proxyInstantRecord(recordType,
        containingRecord, id, instant, attributes);
  }

  @Override
  public <T extends InstantRecord> void deleteInstantRecords(Record containingRecord,
      RecordType<T> recordType, ImmutableList<? extends SearchTerm> searchTerms) throws StorageException {
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    InMemoryRecord inMemoryRecord =
        currentTransaction.getRecord(containingRecord.getRecordType(), containingRecord.getId());
    if (inMemoryRecord == null) {
      inMemoryRecord = getTable(containingRecord.getRecordType()).get(containingRecord.getId());
      if (inMemoryRecord == null) {
        throw new StorageException(
            String.format("Record of type %s and ID %s was not found.",
                containingRecord.getRecordType(), containingRecord.getId()));
      }
      // Promote the stored value to the transaction.
      // This would be overkill for the real storage system, but is easy here.
      currentTransaction.addRecordToTransaction(
          containingRecord.getRecordType(), containingRecord.getId(), inMemoryRecord);
    }
    currentTransaction.deleteInstantRecords(inMemoryRecord, recordType, searchTerms);
  }

  @Override
  public <T extends InstantRecord> Iterable<T> findInstantRecords(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end,
      List<? extends SearchTerm> searchTerms) throws StorageException {
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    InMemoryRecord inMemoryRecord = null;
    if (currentTransaction != null) {
      inMemoryRecord = currentTransaction.getRecord(
          containingRecord.getRecordType(), containingRecord.getId());
    }
    if (inMemoryRecord == null) {
      inMemoryRecord = getTable(containingRecord.getRecordType()).get(containingRecord.getId());
    }
    if (inMemoryRecord == null) {
      throw new StorageException(
          String.format("Record of type %s and ID %s was not found.",
              containingRecord.getRecordType(), containingRecord.getId()));
    }
    Iterable<Map.Entry<InstantRecordKey, Map<String, Object>>> rawRecords =
        inMemoryRecord.getInstantRecords(recordType, start, end);
    ImmutableList.Builder<T> resultsList = ImmutableList.builder();
    for (Map.Entry<InstantRecordKey, Map<String, Object>> entry : rawRecords) {
      InstantRecordKey key = entry.getKey();
      if (matchesSearchTerms(key.getId(), entry.getValue(), searchTerms)) {
        T record = RecordProxy.proxyInstantRecord((RecordType<T>) key.getRecordType(),
            containingRecord, key.getId(), key.getInstant(), entry.getValue());
        resultsList.add(record);
      }
    }
    return resultsList.build();
  }

  static boolean matchesSearchTerms(int id, Map<String, Object> attributes, List<? extends SearchTerm> searchTerms) {
    for (SearchTerm searchTerm : searchTerms) {
      if (searchTerm instanceof AttributeSearchTerm) {
        AttributeSearchTerm attributeSearchTerm = (AttributeSearchTerm) searchTerm;
        Object attribute = attributes.get(attributeSearchTerm.getAttribute());
        if (!attributeSearchTerm.getOperator().matches(attribute, attributeSearchTerm.getValue())) {
          return false;
        }
      } else if (searchTerm instanceof IdentifierSearchTerm) {
        IdentifierSearchTerm identifierSearchTerm = (IdentifierSearchTerm) searchTerm;
        if (identifierSearchTerm.getId() != id) {
          return false;
        }
      } else {
        throw new UnsupportedOperationException(
            "Search terms of type " + searchTerm.getClass() + " are not supported.");
      }
    }
    return true;
  }

  private Map<Integer, InMemoryRecord> getTable(RecordType recordType) {
    Map<Integer, InMemoryRecord> table = tables.get(recordType.getRootType());
    if (table == null) {
      table = new HashMap<>();
      tables.put(recordType.getRootType(), table);
    }
    return table;
  }

  public void commit(Map<RecordType, Map<Integer, InMemoryRecord>> tables) {
    for (Map.Entry<RecordType, Map<Integer, InMemoryRecord>> entry : tables.entrySet()) {
      RecordType recordType = entry.getKey();
      getTable(recordType).putAll(entry.getValue());
    }
  }
}
