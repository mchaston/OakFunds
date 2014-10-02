package org.chaston.oakfunds.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import org.chaston.oakfunds.util.Pair;
import org.joda.time.Instant;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * TODO(mchaston): write JavaDocs
 */
class InMemoryRecord {
  private final ImmutableMap<String, Object> attributes;
  private final Map<RecordType, IntervalRecordSet> intervalRecordSets = new HashMap<>();
  private final Map<RecordType, SortedMap<InstantRecordKey, Map<String, Object>>> instantRecordSets = new HashMap<>();

  public InMemoryRecord(Map<String, Object> attributes) {
    this.attributes = ImmutableMap.copyOf(attributes);
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void updateIntervalRecord(
      RecordInserter recordInserter, RecordType recordType,
      Instant start, Instant end, Map<String, Object> attributes) throws StorageException {
    IntervalRecordSet intervalRecordSet = intervalRecordSets.get(recordType);
    if (intervalRecordSet == null) {
      intervalRecordSet = new IntervalRecordSet();
      intervalRecordSets.put(recordType, intervalRecordSet);
    }
    intervalRecordSet.update(recordInserter, recordType, start, end, attributes);
  }

  public int insertInstantRecord(
      RecordInserter recordInserter, RecordType recordType,
      Instant instant, Map<String, Object> attributes) throws StorageException {
    SortedMap<InstantRecordKey, Map<String, Object>> instantRecordSet = instantRecordSets.get(recordType);
    if (instantRecordSet == null) {
      instantRecordSet = new TreeMap<>();
      instantRecordSets.put(recordType, instantRecordSet);
    }
    int id = recordInserter.insertInstantRecord(recordType, instant, attributes);
    instantRecordSet.put(new InstantRecordKey(instant, id), attributes);
    return id;
  }

  public SortedMap<InstantRecordKey, Map<String, Object>> getInstantRecords(RecordType recordType,
      Instant start, Instant end) {
    SortedMap<InstantRecordKey, Map<String, Object>> instantRecordSet = instantRecordSets.get(recordType);
    if (instantRecordSet == null) {
      return ImmutableSortedMap.of();
    }
    return ImmutableSortedMap.copyOf(
        instantRecordSet.subMap(
            new InstantRecordKey(start, 0), new InstantRecordKey(end, Integer.MAX_VALUE)));
  }

  public Pair<IntervalRecordKey, Map<String, Object>> getIntervalRecord(RecordType recordType,
      Instant date) {
    IntervalRecordSet intervalRecordSet = intervalRecordSets.get(recordType);
    if (intervalRecordSet == null) {
      return null;
    }
    return intervalRecordSet.getIntervalRecord(date);
  }
}
