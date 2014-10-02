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

import org.chaston.oakfunds.util.Pair;
import org.joda.time.Instant;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * TODO(mchaston): write JavaDocs
 */
public class InMemoryStore implements Store {

  public static final ThreadLocal<InMemoryTransaction> CURRENT_TRANSACTION = new ThreadLocal<>();
  public static final Map<Class, Map<String, Method>> WRITE_METHOD_MAPS = new HashMap<>();

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
  public <T extends Record> T createRecord(RecordFactory<T> recordFactory, int id,
      Map<String, Object> attributes) throws StorageException {
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    currentTransaction.insertRecord(recordFactory.getRecordType(), id, attributes);
    return getRecord(recordFactory, id);
  }

  @Override
  public <T extends Record> T createRecord(RecordFactory<T> recordFactory,
      Map<String, Object> attributes) throws StorageException {
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    if (currentTransaction == null) {
      throw new IllegalStateException("Not within transaction.");
    }
    int newId = currentTransaction.insertRecord(recordFactory.getRecordType(), attributes);
    return getRecord(recordFactory, newId);
  }

  @Override
  public <T extends Record> T getRecord(RecordFactory<T> recordFactory, int id)
      throws StorageException {
    InMemoryTransaction currentTransaction = CURRENT_TRANSACTION.get();
    InMemoryRecord inMemoryRecord = null;
    if (currentTransaction != null) {
      inMemoryRecord = currentTransaction.getRecord(recordFactory.getRecordType(), id);
    }
    if (inMemoryRecord == null) {
      inMemoryRecord = getTable(recordFactory.getRecordType()).get(id);
    }
    if (inMemoryRecord == null) {
      throw new StorageException(
          String.format("Record of type %s and ID %s was not found.",
              recordFactory.getRecordType(), id));
    }
    T record = recordFactory.newInstance(id);
    populateRecord(record, inMemoryRecord.getAttributes());
    return record;
  }

  @Override
  public <T extends IntervalRecord> void updateIntervalRecord(Record containingRecord,
      IntervalRecordFactory<T> recordFactory, Instant start, Instant end, Map<String, Object> attributes)
      throws StorageException {
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
    currentTransaction.updateIntervalRecord(
        inMemoryRecord, recordFactory.getRecordType(), start, end, attributes);
  }

  @Override
  public <T extends IntervalRecord> T getIntervalRecord(Record containingRecord,
      IntervalRecordFactory<T> recordFactory, Instant date) throws StorageException {
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
        inMemoryRecord.getIntervalRecord(recordFactory.getRecordType(), date);
    IntervalRecordKey intervalRecordKey = rawRecord.getFirst();
    T record = recordFactory.newInstance(
        intervalRecordKey.getId(), intervalRecordKey.getStart(), intervalRecordKey.getEnd());
    populateRecord(record, rawRecord.getSecond());
    return record;
  }

  @Override
  public <T extends InstantRecord> int insertInstantRecord(Record containingRecord,
      InstantRecordFactory<T> recordFactory, Instant instant, Map<String, Object> attributes)
      throws StorageException {
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
    return currentTransaction.insertInstantRecord(
        inMemoryRecord, recordFactory.getRecordType(), instant, attributes);
  }

  @Override
  public <T extends InstantRecord> Iterable<T> getInstantRecords(Record containingRecord,
      final InstantRecordFactory<T> recordFactory, Instant start, Instant end) throws StorageException {
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
    SortedMap<InstantRecordKey, Map<String, Object>> rawRecords =
        inMemoryRecord.getInstantRecords(recordFactory.getRecordType(), start, end);
    List<T> records = new ArrayList<>();
    for (Map.Entry<InstantRecordKey, Map<String, Object>> entry : rawRecords.entrySet()) {
      T record = recordFactory.newInstance(entry.getKey().getId(), entry.getKey().getInstant());
      populateRecord(record, entry.getValue());
      records.add(record);
    }
    return records;
  }

  private <T extends Record> void populateRecord(T record, Map<String, Object> attributes)
      throws StorageException {
    Map<String, Method> writeMethodMap = loadWriteMethodMap(record.getClass());
    for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
      Method method = writeMethodMap.get(attribute.getKey());
      if (method == null) {
        throw new StorageException(
            String.format("Attribute %s is not a valid attribute on record class %s.",
                attribute.getKey(), record.getClass()));
      }
      try {
        method.invoke(record, attribute.getValue());
      } catch (InvocationTargetException | IllegalAccessException e) {
        throw new StorageException(
            String.format("Attribute %s failed to be written to record class %s.",
                attribute.getKey(), record.getClass()), e);
      }
    }
  }

  private static Map<String, Method> loadWriteMethodMap(Class<? extends Record> recordClass)
      throws StorageException {
    Map<String, Method> writeMethodMap = WRITE_METHOD_MAPS.get(recordClass);
    if (writeMethodMap == null) {
      writeMethodMap = new HashMap<>();
      BeanInfo beanInfo;
      try {
        beanInfo = Introspector.getBeanInfo(recordClass);
      } catch (IntrospectionException e) {
        throw new StorageException(
            String.format("Record class %s could not be introspected.", recordClass), e);
      }
      populateWriteMethodMap(writeMethodMap, beanInfo, recordClass);
      WRITE_METHOD_MAPS.put(recordClass, writeMethodMap);
    }
    return writeMethodMap;
  }

  private static void populateWriteMethodMap(
      Map<String, Method> writeMethodMap, BeanInfo beanInfo, Class<?> recordClass) {
    Field[] fields = recordClass.getDeclaredFields();
    for (Field field : fields) {
      Attribute attribute = field.getAnnotation(Attribute.class);
      if (attribute != null) {
        boolean wasFound = false;
        String propertyName = attribute.propertyName();
        if (propertyName.isEmpty()) {
          propertyName = attribute.name();
        }
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
          if (propertyDescriptor.getName().equals(propertyName)) {
            writeMethodMap.put(attribute.name(), propertyDescriptor.getWriteMethod());
            wasFound = true;
            break;
          }
        }
        if (!wasFound) {
          throw new IllegalStateException(
              String.format("Property %s declared for attribute %s was not found for class %s.",
                  propertyName, attribute.name(), recordClass));
        }
      }
    }
    Class superclass = recordClass.getSuperclass();
    if (superclass != null) {
      populateWriteMethodMap(writeMethodMap, beanInfo, superclass);
    }
  }

  private Map<Integer, InMemoryRecord> getTable(RecordType recordType) {
    Map<Integer, InMemoryRecord> table = tables.get(recordType);
    if (table == null) {
      table = new HashMap<>();
      tables.put(recordType, table);
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
