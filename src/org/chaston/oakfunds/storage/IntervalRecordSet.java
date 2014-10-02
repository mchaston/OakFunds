package org.chaston.oakfunds.storage;

import com.google.common.annotations.VisibleForTesting;
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

  void update(RecordInserter recordInserter, RecordType recordType,
      Instant start, Instant end, Map<String, Object> attributes) throws StorageException {
    // First entry.
    if (records.isEmpty()) {
      insertRecord(recordInserter, recordType, start, end, attributes);
      return;
    }
    // New key is entirely before the first entry.
    Instant existingKeyStart = records.firstKey();
    if (existingKeyStart.isAfter(end) || existingKeyStart.equals(end)) {
      insertRecord(recordInserter, recordType, start, end, attributes);
      return;
    }
    // New key is entirely after the last entry.
    existingKeyStart = records.lastKey();
    Pair<IntervalRecordKey, Map<String, Object>> lastRecord = records.get(existingKeyStart);
    if (lastRecord.getFirst().getEnd().isBefore(start)
        || lastRecord.getFirst().getEnd().equals(start)) {
      insertRecord(recordInserter, recordType, start, end, attributes);
      return;
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
          insertRecord(recordInserter, recordType, start, end, attributes);
          return;
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
    insertRecord(recordInserter, recordType, start, end, attributes);
  }

  @VisibleForTesting
  SortedMap<Instant, Pair<IntervalRecordKey, Map<String, Object>>> getRecords() {
    return records;
  }

  private void insertRecord(RecordInserter recordInserter, RecordType recordType, Instant start,
      Instant end, Map<String, Object> attributes) throws StorageException {
    int id = recordInserter.insertIntervalRecord(recordType, start, end, attributes);
    records.put(start, Pair.of(new IntervalRecordKey(id, start, end), attributes));
  }

  public Pair<IntervalRecordKey, Map<String, Object>> getIntervalRecord(Instant date) {
    SortedMap<Instant, Pair<IntervalRecordKey, Map<String, Object>>> earlierRecords = records.headMap(date.plus(1));
    Instant lastKey = earlierRecords.lastKey();
    return earlierRecords.get(lastKey);
  }
}
