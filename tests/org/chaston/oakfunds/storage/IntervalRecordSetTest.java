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
import org.chaston.oakfunds.util.Pair;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;
import java.util.SortedMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class IntervalRecordSetTest {
  private int counter = 1;
  private RecordInserter recordInserter;

  @Before
  public void setUp() {
    recordInserter = new RecordInserter() {
      @Override
      public int insertRecord(RecordType recordType, Map<String, Object> attributes)
          throws StorageException {
        throw new UnsupportedOperationException();
      }

      @Override
      public void insertRecord(RecordType recordType, int id, Map<String, Object> attributes)
          throws StorageException {
        throw new UnsupportedOperationException();
      }

      @Override
      public int insertInstantRecord(RecordType recordType, Instant date,
          Map<String, Object> attributes) throws StorageException {
        throw new UnsupportedOperationException();
      }

      @Override
      public int insertIntervalRecord(RecordType recordType, Instant start, Instant end,
          Map<String, Object> attributes) throws StorageException {
        assertEquals(RecordType.ACCOUNT, recordType);
        return counter++;
      }
    };
  }

  @Test
  public void initiallyEmpty() {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();

    assertEquals(0, intervalRecordSet.getRecords().size());
  }

  @Test
  public void simpleInsert() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));

    assertEquals(1, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 1,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
  }

  @Test
  public void insertBefore() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2013-01-01"), Instant.parse("2013-11-01"), createAttributes(2));

    assertEquals(2, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 1,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    assertContainsRecord(intervalRecordSet.getRecords(), 2,
        Instant.parse("2013-01-01"), Instant.parse("2013-11-01"), createAttributes(2));
  }

  @Test
  public void insertImmediatelyBefore() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2013-01-01"), Instant.parse("2014-01-01"), createAttributes(2));

    assertEquals(2, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 1,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    assertContainsRecord(intervalRecordSet.getRecords(), 2,
        Instant.parse("2013-01-01"), Instant.parse("2014-01-01"), createAttributes(2));
  }

  @Test
  public void insertAfter() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2015-02-01"), Instant.parse("2016-01-01"), createAttributes(2));

    assertEquals(2, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 1,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    assertContainsRecord(intervalRecordSet.getRecords(), 2,
        Instant.parse("2015-02-01"), Instant.parse("2016-01-01"), createAttributes(2));
  }

  @Test
  public void insertAfterBefore() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2015-01-01"), Instant.parse("2016-01-01"), createAttributes(2));

    assertEquals(2, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 1,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    assertContainsRecord(intervalRecordSet.getRecords(), 2,
        Instant.parse("2015-01-01"), Instant.parse("2016-01-01"), createAttributes(2));
  }

  @Test
  public void insertReplace() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(2));

    assertEquals(1, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 2,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(2));
  }

  @Test
  public void insertOverlapStart() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2013-01-01"), Instant.parse("2014-06-01"), createAttributes(2));

    assertEquals(2, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 2,
        Instant.parse("2014-06-01"), Instant.parse("2015-01-01"), createAttributes(1));
    assertContainsRecord(intervalRecordSet.getRecords(), 3,
        Instant.parse("2013-01-01"), Instant.parse("2014-06-01"), createAttributes(2));
  }

  @Test
  public void insertOverlapEnd() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-06-01"), Instant.parse("2016-01-01"), createAttributes(2));

    assertEquals(2, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 1,
        Instant.parse("2014-01-01"), Instant.parse("2014-06-01"), createAttributes(1));
    assertContainsRecord(intervalRecordSet.getRecords(), 2,
        Instant.parse("2014-06-01"), Instant.parse("2016-01-01"), createAttributes(2));
  }

  @Test
  public void insertOverlapComplete() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(1));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2013-01-01"), Instant.parse("2016-01-01"), createAttributes(2));

    assertEquals(1, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 2,
        Instant.parse("2013-01-01"), Instant.parse("2016-01-01"), createAttributes(2));
  }

  @Test
  public void insertBetween() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2012-01-01"), Instant.parse("2013-01-01"), createAttributes(1));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(2));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2013-01-01"), Instant.parse("2014-01-01"), createAttributes(3));

    assertEquals(3, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 1,
        Instant.parse("2012-01-01"), Instant.parse("2013-01-01"), createAttributes(1));
    assertContainsRecord(intervalRecordSet.getRecords(), 2,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(2));
    assertContainsRecord(intervalRecordSet.getRecords(), 3,
        Instant.parse("2013-01-01"), Instant.parse("2014-01-01"), createAttributes(3));
  }

  @Test
  public void insertOverlapStraddle() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2012-01-01"), Instant.parse("2013-01-01"), createAttributes(1));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(2));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2012-06-01"), Instant.parse("2014-06-01"), createAttributes(3));

    assertEquals(3, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 1,
        Instant.parse("2012-01-01"), Instant.parse("2012-06-01"), createAttributes(1));
    assertContainsRecord(intervalRecordSet.getRecords(), 3,
        Instant.parse("2014-06-01"), Instant.parse("2015-01-01"), createAttributes(2));
    assertContainsRecord(intervalRecordSet.getRecords(), 4,
        Instant.parse("2012-06-01"), Instant.parse("2014-06-01"), createAttributes(3));
  }

  @Test
  public void insertOverlapStraddleComplex() throws StorageException {
    IntervalRecordSet intervalRecordSet = new IntervalRecordSet();
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2012-01-01"), Instant.parse("2013-01-01"), createAttributes(1));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-01-01"), Instant.parse("2015-01-01"), createAttributes(2));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2016-01-01"), Instant.parse("2017-01-01"), createAttributes(3));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2018-01-01"), Instant.parse("2019-01-01"), createAttributes(4));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2020-01-01"), Instant.parse("2021-01-01"), createAttributes(5));
    intervalRecordSet.update(recordInserter, RecordType.ACCOUNT,
        Instant.parse("2014-06-01"), Instant.parse("2018-06-01"), createAttributes(6));

    assertEquals(5, intervalRecordSet.getRecords().size());

    assertContainsRecord(intervalRecordSet.getRecords(), 1,
        Instant.parse("2012-01-01"), Instant.parse("2013-01-01"), createAttributes(1));
    assertContainsRecord(intervalRecordSet.getRecords(), 2,
        Instant.parse("2014-01-01"), Instant.parse("2014-06-01"), createAttributes(2));
    assertContainsRecord(intervalRecordSet.getRecords(), 6,
        Instant.parse("2018-06-01"), Instant.parse("2019-01-01"), createAttributes(4));
    assertContainsRecord(intervalRecordSet.getRecords(), 5,
        Instant.parse("2020-01-01"), Instant.parse("2021-01-01"), createAttributes(5));
    assertContainsRecord(intervalRecordSet.getRecords(), 7,
        Instant.parse("2014-06-01"), Instant.parse("2018-06-01"), createAttributes(6));
  }

  private void assertContainsRecord(
      SortedMap<Instant, Pair<IntervalRecordKey, Map<String, Object>>> records,
      int id, Instant start, Instant end, Map<String, Object> attributes) {
    Pair<IntervalRecordKey, Map<String, Object>> record = records.get(start);
    assertNotNull(record);
    IntervalRecordKey key = record.getFirst();
    assertEquals(id, key.getId());
    assertEquals(start, key.getStart());
    assertEquals(end, key.getEnd());
    assertEquals(attributes, record.getSecond());
  }

  private Map<String, Object> createAttributes(int seed) {
    return ImmutableMap.of("value", (Object) seed);
  }
}
