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

import javax.annotation.Nullable;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class InMemoryStore extends AbstractStore {

  public static final ThreadLocal<InMemoryTransaction> CURRENT_TRANSACTION = new ThreadLocal<>();
  public static final Map<Class, Map<String, Method>> WRITE_METHOD_MAPS = new HashMap<>();
  public static final Map<Class, Method> PARENT_IDENTIFIER_WRITE_METHODS = new HashMap<>();

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
    T record = getRecordFactory(recordType).newInstance(inMemoryRecord.getRecordType(), id);
    populateRecord(record, inMemoryRecord.getAttributes(), null);
    return record;
  }

  @Override
  public <T extends Record> T updateRecord(T record, Map<String, Object> attributes)
      throws StorageException {
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
    RecordFactory<T> recordFactory = getRecordFactory(record.getRecordType());
    T newRecord = recordFactory.newInstance(record.getRecordType(), record.getId());
    populateRecord(newRecord, inMemoryRecord.getAttributes(), null);
    return newRecord;
  }

  @Override
  public <T extends Record> Iterable<T> findRecords(RecordType<T> recordType,
      List<SearchTerm> searchTerms) throws StorageException {
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
    RecordFactory<T> recordFactory = getRecordFactory(recordType);
    for (Map.Entry<Integer, InMemoryRecord> entry : results.entrySet()) {
      InMemoryRecord inMemoryRecord = entry.getValue();
      T record = recordFactory.newInstance(inMemoryRecord.getRecordType(), entry.getKey());
      populateRecord(record, entry.getValue().getAttributes(), null);
      resultList.add(record);
    }
    return resultList.build();
  }

  @Override
  public <T extends IntervalRecord> T updateIntervalRecord(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end, Map<String, Object> attributes)
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
    int id = currentTransaction.updateIntervalRecord(
        inMemoryRecord, recordType, start, end, attributes);
    T record = getIntervalRecordFactory(recordType).newInstance(recordType, id, start, end);
    populateRecord(record, attributes, containingRecord);
    return record;
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
    T record = getIntervalRecordFactory(recordType).newInstance(
        intervalRecordKey.getRecordType(),
        intervalRecordKey.getId(), intervalRecordKey.getStart(), intervalRecordKey.getEnd());
    populateRecord(record, rawRecord.getSecond(), containingRecord);
    return record;
  }

  @Override
  public <T extends IntervalRecord> Iterable<T> findIntervalRecords(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end, List<SearchTerm> searchTerms)
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
    IntervalRecordFactory<T> intervalRecordFactory = getIntervalRecordFactory(recordType);
    ImmutableList.Builder<T> resultList = ImmutableList.builder();
    for (Pair<IntervalRecordKey, Map<String, Object>> rawRecord : rawRecords) {
      if (matchesSearchTerms(rawRecord.getSecond(), searchTerms)) {
        IntervalRecordKey intervalRecordKey = rawRecord.getFirst();
        T record = intervalRecordFactory.newInstance(
            intervalRecordKey.getRecordType(),
            intervalRecordKey.getId(), intervalRecordKey.getStart(), intervalRecordKey.getEnd());
        populateRecord(record, rawRecord.getSecond(), containingRecord);
        resultList.add(record);
      }
    }
    return resultList.build();
  }

  @Override
  public <T extends InstantRecord> T insertInstantRecord(Record containingRecord,
      RecordType<T> recordType, Instant instant, Map<String, Object> attributes)
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
    int id = currentTransaction.insertInstantRecord(
        inMemoryRecord, recordType, instant, attributes);
    T record = getInstantRecordFactory(recordType).newInstance(recordType, id, instant);
    populateRecord(record, attributes, containingRecord);
    return record;
  }

  @Override
  public <T extends InstantRecord> Iterable<T> findInstantRecords(Record containingRecord,
      RecordType<T> recordType, Instant start, Instant end,
      List<SearchTerm> searchTerms) throws StorageException {
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
    InstantRecordFactory<T> instantRecordFactory = getInstantRecordFactory(recordType);
    for (Map.Entry<InstantRecordKey, Map<String, Object>> entry : rawRecords) {
      if (matchesSearchTerms(entry.getValue(), searchTerms)) {
        InstantRecordKey key = entry.getKey();
        T record = instantRecordFactory.newInstance(key.getRecordType(), key.getId(),
            key.getInstant());
        populateRecord(record, entry.getValue(), containingRecord);
        resultsList.add(record);
      }
    }
    return resultsList.build();
  }

  static boolean matchesSearchTerms(Map<String, Object> attributes, List<SearchTerm> searchTerms) {
    for (SearchTerm searchTerm : searchTerms) {
      Object attribute = attributes.get(searchTerm.getAttribute());
      if (!searchTerm.getOperator().matches(attribute, searchTerm.getValue())) {
        return false;
      }
    }
    return true;
  }

  private <T extends Record> void populateRecord(
      T record, Map<String, Object> attributes, @Nullable Record parentRecord)
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
    if (parentRecord != null) {
      Method parentIdentifierWriteMethod = loadParentIdentifierWriteMethod(record.getClass());
      if (parentIdentifierWriteMethod != null) {
        try {
          parentIdentifierWriteMethod.invoke(record, parentRecord.getId());
        } catch (InvocationTargetException | IllegalAccessException e) {
          throw new StorageException(
              String.format("Parent ID failed to be written to record class %s.",
                  record.getClass()), e);
        }
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

  @Nullable
  private static Method loadParentIdentifierWriteMethod(Class<? extends Record> recordClass)
      throws StorageException {
    Method parentIdentifierWriteMethod = PARENT_IDENTIFIER_WRITE_METHODS.get(recordClass);
    if (parentIdentifierWriteMethod == null
        && !PARENT_IDENTIFIER_WRITE_METHODS.containsKey(recordClass)) {
      BeanInfo beanInfo;
      try {
        beanInfo = Introspector.getBeanInfo(recordClass);
      } catch (IntrospectionException e) {
        throw new StorageException(
            String.format("Record class %s could not be introspected.", recordClass), e);
      }
      parentIdentifierWriteMethod = readParentIdentifierWriteMethod(beanInfo, recordClass);
      PARENT_IDENTIFIER_WRITE_METHODS.put(recordClass, parentIdentifierWriteMethod);
    }
    return parentIdentifierWriteMethod;
  }

  private static Method readParentIdentifierWriteMethod(BeanInfo beanInfo,
      Class<? extends Record> recordClass) {
    Field[] fields = recordClass.getDeclaredFields();
    for (Field field : fields) {
      ParentIdAttribute attribute = field.getAnnotation(ParentIdAttribute.class);
      if (attribute != null) {
        String propertyName = attribute.propertyName();
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
          if (propertyDescriptor.getName().equals(propertyName)) {
            return propertyDescriptor.getWriteMethod();
          }
        }
        throw new IllegalStateException(
            String.format("Property %s declared for field %s was not found for class %s.",
                propertyName, field.getName(), recordClass));
      }
    }
    Class superclass = recordClass.getSuperclass();
    if (superclass != null) {
      return readParentIdentifierWriteMethod(beanInfo, superclass);
    }
    return null;
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
