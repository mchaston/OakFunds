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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.chaston.oakfunds.util.Pair;
import org.joda.time.Instant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * TODO(mchaston): write JavaDocs
 */
class InMemoryRecord {
  private final RecordType recordType;
  private final int id;
  private final ImmutableMap<String, Object> attributes;
  private final Map<RecordType, IntervalRecordSet> intervalRecordSets = new HashMap<>();
  private final Map<RecordType, SortedMap<InstantRecordKey, Map<String, Object>>> instantRecordSets = new HashMap<>();

  public InMemoryRecord(RecordType recordType, int id, Map<String, Object> attributes) {
    this.recordType = recordType;
    this.id = id;
    this.attributes = ImmutableMap.copyOf(attributes);
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void updateRecord(Map<String, Object> attributes) {
    attributes.putAll(attributes);
  }

  public int updateIntervalRecord(
      RecordInserter recordInserter, RecordType recordType,
      Instant start, Instant end, Map<String, Object> attributes) throws StorageException {
    IntervalRecordSet intervalRecordSet = intervalRecordSets.get(recordType.getRootType());
    if (intervalRecordSet == null) {
      intervalRecordSet = new IntervalRecordSet();
      intervalRecordSets.put(recordType.getRootType(), intervalRecordSet);
    }
    return intervalRecordSet.update(recordInserter, recordType, start, end, attributes);
  }

  public int insertInstantRecord(
      RecordInserter recordInserter, RecordType recordType,
      Instant instant, Map<String, Object> attributes) throws StorageException {
    SortedMap<InstantRecordKey, Map<String, Object>> instantRecordSet = instantRecordSets.get(recordType.getRootType());
    if (instantRecordSet == null) {
      instantRecordSet = new TreeMap<>();
      instantRecordSets.put(recordType.getRootType(), instantRecordSet);
    }
    int id = recordInserter.insertInstantRecord(recordType, instant, attributes);
    instantRecordSet.put(new InstantRecordKey(recordType, instant, id), attributes);
    return id;
  }

  public void updateInstantRecord(RecordType recordType, int id,
      Instant instant, Map<String, Object> attributes) throws StorageException {
    SortedMap<InstantRecordKey, Map<String, Object>> instantRecordSet = instantRecordSets.get(recordType.getRootType());
    if (instantRecordSet == null) {
      throw new StorageException("No records of type " + recordType + " found.");
    }
    for (Map.Entry<InstantRecordKey, Map<String, Object>> entry : instantRecordSet.entrySet()) {
      if (entry.getKey().getId() == id) {
        instantRecordSet.remove(entry.getKey());
        instantRecordSet.put(new InstantRecordKey(recordType, instant, id), attributes);
        return;
      }
    }
    throw new StorageException("No record of type " + recordType + " and ID " + id + " found.");
  }

  public void deleteInstantRecords(RecordType recordType, ImmutableList<? extends SearchTerm> searchTerms) {
    SortedMap<InstantRecordKey, Map<String, Object>> instantRecordSet = instantRecordSets.get(recordType.getRootType());
    if (instantRecordSet == null) {
      return;
    }
    Iterator<Map.Entry<InstantRecordKey, Map<String, Object>>> iterator =
        instantRecordSet.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<InstantRecordKey, Map<String, Object>> entry = iterator.next();
      int id = entry.getKey().getId();
      Map<String, Object> attributes = entry.getValue();
      if (InMemoryStore.matchesSearchTerms(this.id, id, attributes, searchTerms)) {
        iterator.remove();
      }
    }
  }

  public Iterable<Map.Entry<InstantRecordKey, Map<String, Object>>> getInstantRecords(
      final RecordType recordType, Instant start, Instant end) {
    SortedMap<InstantRecordKey, Map<String, Object>> instantRecordSet = instantRecordSets.get(recordType.getRootType());
    if (instantRecordSet == null) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(
        Sets.filter(
            instantRecordSet.subMap(
                new InstantRecordKey(recordType, start, 0),
                new InstantRecordKey(recordType, end, 0))
                    .entrySet(),
            new Predicate<Map.Entry<InstantRecordKey, Map<String, Object>>>() {
              @Override
              public boolean apply(Map.Entry<InstantRecordKey, Map<String, Object>> entry) {
                return entry.getKey().getRecordType().isTypeOf(recordType);
              }
            }));
  }

  public Pair<IntervalRecordKey, Map<String, Object>> getIntervalRecord(RecordType recordType,
      Instant date) {
    IntervalRecordSet intervalRecordSet = intervalRecordSets.get(recordType.getRootType());
    if (intervalRecordSet == null) {
      return null;
    }
    Pair<IntervalRecordKey, Map<String, Object>> intervalRecord =
        intervalRecordSet.getIntervalRecord(date);
    if (intervalRecord.getFirst().getRecordType().isTypeOf(recordType)) {
      return intervalRecord;
    }
    return null;
  }

  public <T extends IntervalRecord> Iterable<Pair<IntervalRecordKey, Map<String, Object>>>
      getIntervalRecords(final RecordType<T> recordType, Instant start, Instant end) {
    IntervalRecordSet intervalRecordSet = intervalRecordSets.get(recordType.getRootType());
    if (intervalRecordSet == null) {
      return ImmutableList.of();
    }
    return Iterables.filter(
        intervalRecordSet.getIntervalRecords(start, end),
        new Predicate<Pair<IntervalRecordKey, Map<String, Object>>>() {
          @Override
          public boolean apply(Pair<IntervalRecordKey, Map<String, Object>> entry) {
            return entry.getFirst().getRecordType().isTypeOf(recordType);
          }
        });
  }

  public boolean matchesSearchTerms(List<? extends SearchTerm> searchTerms) {
    return InMemoryStore.matchesSearchTerms(null, id, attributes, searchTerms);
  }

  public RecordType getRecordType() {
    return recordType;
  }

  public int getId() {
    return id;
  }
}
