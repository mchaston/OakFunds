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
package org.chaston.oakfunds.storage.mgmt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.chaston.oakfunds.jdbc.ColumnDef;
import org.chaston.oakfunds.jdbc.TableDef;
import org.chaston.oakfunds.storage.AttributeMethod;
import org.chaston.oakfunds.storage.Identifiable;
import org.chaston.oakfunds.storage.InstantRecord;
import org.chaston.oakfunds.storage.IntervalRecord;
import org.chaston.oakfunds.storage.ParentIdMethod;
import org.chaston.oakfunds.storage.Record;
import org.chaston.oakfunds.storage.RecordTemporalType;
import org.chaston.oakfunds.storage.RecordType;
import org.chaston.oakfunds.storage.SystemColumnDefs;
import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class SchemaBuilderTest {

  @Test
  public void getTableDefsForRecord() {
    ImmutableSet<RecordType> recordTypes = ImmutableSet.<RecordType>of(TestSimpleRecord.TYPE);
    SchemaBuilder schemaBuilder = new SchemaBuilder(recordTypes);

    ImmutableMap<String, TableDef> tableDefs = schemaBuilder.getTableDefs();
    assertEquals(1, tableDefs.size());
    TableDef tableDef = tableDefs.get("simple_record");
    assertEquals("simple_record", tableDef.getName());

    ImmutableMap<String, ColumnDef> columnDefs = tableDef.getColumnDefs();
    assertEquals(9, columnDefs.size());

    assertSame(SystemColumnDefs.ID, columnDefs.get("sys_id"));
    assertSame(SystemColumnDefs.TYPE, columnDefs.get("sys_type"));

    assertContainsColumn(columnDefs, "name", Types.VARCHAR, true);
    assertContainsColumn(columnDefs, "string", Types.VARCHAR, false);
    assertContainsColumn(columnDefs, "boolean", Types.BOOLEAN, false);
    assertContainsColumn(columnDefs, "int", Types.INTEGER, false);
    assertContainsColumn(columnDefs, "date", Types.TIMESTAMP, false);
    assertContainsColumn(columnDefs, "big_decimal", Types.BIGINT, false);
    assertContainsColumn(columnDefs, "identifiable", Types.SMALLINT, false);
  }

  @Test
  public void getTableDefsForRecordWithSubtypes() {
    ImmutableSet<RecordType> recordTypes = ImmutableSet.<RecordType>of(
        TestRootRecord.TYPE,
        TestSubRecord1.TYPE,
        TestSubRecord2.TYPE,
        TestSubRecord21.TYPE);
    SchemaBuilder schemaBuilder = new SchemaBuilder(recordTypes);

    ImmutableMap<String, TableDef> tableDefs = schemaBuilder.getTableDefs();
    assertEquals(1, tableDefs.size());
    TableDef tableDef = tableDefs.get("complex_record");
    assertEquals("complex_record", tableDef.getName());

    ImmutableMap<String, ColumnDef> columnDefs = tableDef.getColumnDefs();
    assertEquals(6, columnDefs.size());

    assertSame(SystemColumnDefs.ID, columnDefs.get("sys_id"));
    assertSame(SystemColumnDefs.TYPE, columnDefs.get("sys_type"));

    assertContainsColumn(columnDefs, "name", Types.VARCHAR, true);
    assertContainsColumn(columnDefs, "sub1__string", Types.VARCHAR, false);
    assertContainsColumn(columnDefs, "sub2__string", Types.VARCHAR, false);
    assertContainsColumn(columnDefs, "sub21__other_string", Types.VARCHAR, false);
  }

  @Test
  public void getTableDefsForInstantRecord() {
    ImmutableSet<RecordType> recordTypes = ImmutableSet.<RecordType>of(
        TestSimpleRecord.TYPE,
        TestInstantRecord.TYPE);
    SchemaBuilder schemaBuilder = new SchemaBuilder(recordTypes);

    ImmutableMap<String, TableDef> tableDefs = schemaBuilder.getTableDefs();
    assertEquals(2, tableDefs.size());
    TableDef tableDef = tableDefs.get("instant_record");
    assertEquals("instant_record", tableDef.getName());

    ImmutableMap<String, ColumnDef> columnDefs = tableDef.getColumnDefs();
    assertEquals(5, columnDefs.size());

    assertSame(SystemColumnDefs.ID, columnDefs.get("sys_id"));
    assertSame(SystemColumnDefs.TYPE, columnDefs.get("sys_type"));
    assertSame(SystemColumnDefs.INSTANT, columnDefs.get("sys_instant"));
    assertSame(SystemColumnDefs.PARENT_ID, columnDefs.get("sys_parent_id"));

    assertContainsColumn(columnDefs, "big_decimal", Types.BIGINT, false);
  }

  @Test
  public void getTableDefsForIntervalRecord() {
    ImmutableSet<RecordType> recordTypes = ImmutableSet.<RecordType>of(
        TestSimpleRecord.TYPE,
        TestIntervalRecord.TYPE);
    SchemaBuilder schemaBuilder = new SchemaBuilder(recordTypes);

    ImmutableMap<String, TableDef> tableDefs = schemaBuilder.getTableDefs();
    assertEquals(2, tableDefs.size());
    TableDef tableDef = tableDefs.get("interval_record");
    assertEquals("interval_record", tableDef.getName());

    ImmutableMap<String, ColumnDef> columnDefs = tableDef.getColumnDefs();
    assertEquals(6, columnDefs.size());

    assertSame(SystemColumnDefs.ID, columnDefs.get("sys_id"));
    assertSame(SystemColumnDefs.TYPE, columnDefs.get("sys_type"));
    assertSame(SystemColumnDefs.START_TIME, columnDefs.get("sys_start_time"));
    assertSame(SystemColumnDefs.END_TIME, columnDefs.get("sys_end_time"));
    assertSame(SystemColumnDefs.PARENT_ID, columnDefs.get("sys_parent_id"));

    assertContainsColumn(columnDefs, "big_decimal", Types.BIGINT, false);
  }

  private void assertContainsColumn(ImmutableMap<String, ColumnDef> columnDefs,
      String name, int type, boolean required) {
    ColumnDef columnDef = columnDefs.get(name);
    assertEquals(name, columnDef.getName());
    assertEquals(type, columnDef.getType());
    assertEquals(required, columnDef.isRequired());
  }

  private interface TestSimpleRecord extends Record<TestSimpleRecord> {
    final RecordType<TestSimpleRecord> TYPE =
        new RecordType<>("simple_record", TestSimpleRecord.class,
            null, RecordTemporalType.NONE, true);

    @AttributeMethod(attribute = "name", required = true)
    String getName();

    @AttributeMethod(attribute = "string")
    String getString();

    @AttributeMethod(attribute = "boolean")
    boolean getBoolean();

    @AttributeMethod(attribute = "int")
    int getInt();

    @AttributeMethod(attribute = "date")
    Instant getDate();

    @AttributeMethod(attribute = "big_decimal")
    BigDecimal getBigDecimal();

    @AttributeMethod(attribute = "identifiable")
    CustomEnum getCustomEnum();
  }

  private enum CustomEnum implements Identifiable {
    FIRST {
      @Override
      public byte identifier() {
        return 1;
      }
    }, SECOND {
      @Override
      public byte identifier() {
        return 2;
      }
    }, THIRD {
      @Override
      public byte identifier() {
        return 3;
      }
    }
  }

  private interface TestRootRecord<T extends TestRootRecord> extends Record<T> {
    final RecordType<TestRootRecord> TYPE =
        new RecordType<>("complex_record", TestRootRecord.class,
            null, RecordTemporalType.NONE, false);

    @AttributeMethod(attribute = "name", required = true)
    String getName();
  }

  private interface TestSubRecord1 extends TestRootRecord<TestSubRecord1> {
    final RecordType<TestSubRecord1> TYPE =
        new RecordType<>("sub1", TestSubRecord1.class,
            TestRootRecord.TYPE, true);

    @AttributeMethod(attribute = "string")
    String getString();
  }

  private interface TestSubRecord2<T extends TestSubRecord2> extends TestRootRecord<T> {
    final RecordType<TestSubRecord2> TYPE =
        new RecordType<>("sub2", TestSubRecord2.class,
            TestRootRecord.TYPE, false);

    @AttributeMethod(attribute = "string")
    String getString();
  }

  private interface TestSubRecord21 extends TestSubRecord2<TestSubRecord21> {
    final RecordType<TestSubRecord21> TYPE =
        new RecordType<>("sub21", TestSubRecord21.class,
            TestSubRecord2.TYPE, true);

    @AttributeMethod(attribute = "other_string")
    String getOtherString();
  }

  private interface TestInstantRecord extends InstantRecord<TestInstantRecord> {
    final RecordType<TestInstantRecord> TYPE =
        new RecordType<>("instant_record", TestInstantRecord.class,
            TestSimpleRecord.TYPE, RecordTemporalType.INSTANT, true);

    @ParentIdMethod
    int getSimpleRecordId();

    @AttributeMethod(attribute = "big_decimal")
    BigDecimal getBigDecimal();
  }

  private interface TestIntervalRecord extends IntervalRecord<TestIntervalRecord> {
    final RecordType<TestIntervalRecord> TYPE =
        new RecordType<>("interval_record", TestIntervalRecord.class,
            TestSimpleRecord.TYPE, RecordTemporalType.INTERVAL, true);

    @ParentIdMethod
    int getSimpleRecordId();

    @AttributeMethod(attribute = "big_decimal")
    BigDecimal getBigDecimal();
  }
}
