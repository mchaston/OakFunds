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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.chaston.oakfunds.util.Pair;
import org.joda.time.Instant;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * TODO(mchaston): write JavaDocs
 */
class IntervalRecordSet {

  private SortedMap<Instant, Pair<IntervalRecordKey, Map<String, Object>>> records =
      new TreeMap<>();

  int update(RecordInserter recordInserter, RecordType recordType,
      Instant start, Instant end, Map<String, Object> attributes) throws StorageException {
    // First entry.
    if (records.isEmpty()) {
      return insertRecord(recordInserter, recordType, start, end, attributes);
    }
    // New key is entirely before the first entry.
    Instant existingKeyStart = records.firstKey();
    if (existingKeyStart.isAfter(end) || existingKeyStart.equals(end)) {
      return insertRecord(recordInserter, recordType, start, end, attributes);
    }
    // New key is entirely after the last entry.
    existingKeyStart = records.lastKey();
    Pair<IntervalRecordKey, Map<String, Object>> lastRecord = records.get(existingKeyStart);
    if (lastRecord.getFirst().getEnd().isBefore(start)
        || lastRecord.getFirst().getEnd().equals(start)) {
      return insertRecord(recordInserter, recordType, start, end, attributes);
    }
    // Work with keys that start before this does.
    SortedMap<Instant, Pair<IntervalRecordKey, Map<String, Object>>> beforeStart =
        records.headMap(start);
    if (!beforeStart.isEmpty()) {
      existingKeyStart = beforeStart.lastKey();
      lastRecord = records.get(existingKeyStart);
      if (lastRecord.getFirst().getEnd().isBefore(start)
          || lastRecord.getFirst().getEnd().equals(start)) {
        // Entirely before the new key.
      } else {
        if (lastRecord.getFirst().getEnd().isBefore(end)
            || lastRecord.getFirst().getEnd().equals(end)) {
          // Need to truncate the last key.
          lastRecord.getFirst().setEnd(start);
        } else {
          // The original must end after the new one, so copy and truncate the original, splitting the
          // key's interval.
          insertRecord(recordInserter, recordType, end, lastRecord.getFirst().getEnd(),
              lastRecord.getSecond());
          lastRecord.getFirst().setEnd(start);
          return insertRecord(recordInserter, recordType, start, end, attributes);
        }
      }
    }
    // Work with keys that are within the new range.
    SortedMap<Instant, Pair<IntervalRecordKey, Map<String, Object>>> during =
        records.subMap(start, end);
    if (!during.isEmpty()) {
      existingKeyStart = during.lastKey();
      lastRecord = records.get(existingKeyStart);
      if (lastRecord.getFirst().getEnd().isAfter(end)) {
        // The last one ends after the new one, so copy the values into a new interval.
        insertRecord(recordInserter, recordType, end, lastRecord.getFirst().getEnd(),
            lastRecord.getSecond());
      }
      // Remove everything in the overlapping set.
      during.clear();
    }
    return insertRecord(recordInserter, recordType, start, end, attributes);
  }

  @VisibleForTesting
  SortedMap<Instant, Pair<IntervalRecordKey, Map<String, Object>>> getRecords() {
    return records;
  }

  private int insertRecord(RecordInserter recordInserter, RecordType recordType, Instant start,
      Instant end, Map<String, Object> attributes) throws StorageException {
    int id = recordInserter.insertIntervalRecord(recordType, start, end, attributes);
    records.put(start, Pair.of(new IntervalRecordKey(recordType, id, start, end), attributes));
    return id;
  }

  public Pair<IntervalRecordKey, Map<String, Object>> getIntervalRecord(Instant date) {
    SortedMap<Instant, Pair<IntervalRecordKey, Map<String, Object>>> earlierRecords =
        records.headMap(date.plus(1));
    Instant lastKey = earlierRecords.lastKey();
    return earlierRecords.get(lastKey);
  }

  public Iterable<Pair<IntervalRecordKey, Map<String, Object>>> getIntervalRecords(
      Instant start, Instant end) {
    ImmutableList.Builder<Pair<IntervalRecordKey, Map<String, Object>>> resultList =
        ImmutableList.builder();
    SortedMap<Instant, Pair<IntervalRecordKey, Map<String, Object>>> earlierRecords =
        records.headMap(start);
    if (!earlierRecords.isEmpty()) {
      resultList.add(earlierRecords.get(earlierRecords.lastKey()));
    }
    resultList.addAll(records.subMap(start, end).values());
    return resultList.build();
  }
}
