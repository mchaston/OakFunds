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
import org.chaston.oakfunds.jdbc.FunctionDef;
import org.chaston.oakfunds.jdbc.TableDef;
import org.chaston.oakfunds.storage.AttributeMethod;
import org.chaston.oakfunds.storage.EnumIdentifiableSource;
import org.chaston.oakfunds.storage.Identifiable;
import org.chaston.oakfunds.storage.IdentifiableSource;
import org.chaston.oakfunds.storage.InstantRecord;
import org.chaston.oakfunds.storage.IntervalRecord;
import org.chaston.oakfunds.storage.ParentIdMethod;
import org.chaston.oakfunds.storage.Record;
import org.chaston.oakfunds.storage.RecordType;
import org.chaston.oakfunds.storage.SystemColumnDefs;
import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class SchemaBuilderTest {

  @Test
  public void getTableDefsForRecord() {
    ImmutableSet<RecordType> recordTypes = ImmutableSet.<RecordType>of(TestSimpleRecord.TYPE);
    SchemaBuilder schemaBuilder = new SchemaBuilder(recordTypes, ImmutableSet.<FunctionDef>of());

    ImmutableMap<String, TableDef> tableDefs = schemaBuilder.getTableDefs();
    assertEquals(1, tableDefs.size());
    TableDef tableDef = tableDefs.get("simple_record");
    assertEquals("simple_record", tableDef.getName());
    assertEquals(SystemColumnDefs.SCHEMA + ".simple_record", tableDef.getFullName());

    ImmutableMap<String, ColumnDef> columnDefs = tableDef.getColumnDefs();
    assertEquals(9, columnDefs.size());

    assertSame(SystemColumnDefs.MANUAL_ID, columnDefs.get("sys_id"));
    assertSame(SystemColumnDefs.TYPE, columnDefs.get("sys_type"));

    assertContainsColumn(columnDefs, "usr_name", Types.VARCHAR);
    assertContainsColumn(columnDefs, "usr_string", Types.VARCHAR);
    assertContainsColumn(columnDefs, "usr_boolean", Types.BOOLEAN);
    assertContainsColumn(columnDefs, "usr_int", Types.INTEGER);
    assertContainsColumn(columnDefs, "usr_date", Types.TIMESTAMP);
    assertContainsColumn(columnDefs, "usr_big_decimal", Types.BIGINT);
    assertContainsColumn(columnDefs, "usr_identifiable", Types.SMALLINT);
  }

  @Test
  public void getTableDefsForRecordWithSubtypes() {
    ImmutableSet<RecordType> recordTypes = ImmutableSet.<RecordType>of(
        TestRootRecord.TYPE,
        TestSubRecord1.TYPE,
        TestSubRecord11.TYPE,
        TestSubRecord2.TYPE);
    SchemaBuilder schemaBuilder = new SchemaBuilder(recordTypes, ImmutableSet.<FunctionDef>of());

    ImmutableMap<String, TableDef> tableDefs = schemaBuilder.getTableDefs();
    assertEquals(1, tableDefs.size());
    TableDef tableDef = tableDefs.get("complex_record");
    assertEquals("complex_record", tableDef.getName());
    assertEquals(SystemColumnDefs.SCHEMA + ".complex_record", tableDef.getFullName());

    ImmutableMap<String, ColumnDef> columnDefs = tableDef.getColumnDefs();
    assertEquals(6, columnDefs.size());

    assertSame(SystemColumnDefs.AUTO_NUMBERED_ID, columnDefs.get("sys_id"));
    assertSame(SystemColumnDefs.TYPE, columnDefs.get("sys_type"));

    assertContainsColumn(columnDefs, "usr_name", Types.VARCHAR);
    assertContainsColumn(columnDefs, "usr_sub1__string", Types.VARCHAR);
    assertContainsColumn(columnDefs, "usr_sub11__other_string", Types.VARCHAR);
    assertContainsColumn(columnDefs, "usr_sub2__string", Types.VARCHAR);
  }

  @Test
  public void getTableDefsForInstantRecord() {
    ImmutableSet<RecordType> recordTypes = ImmutableSet.<RecordType>of(
        TestSimpleRecord.TYPE,
        TestInstantRecord.TYPE);
    SchemaBuilder schemaBuilder = new SchemaBuilder(recordTypes, ImmutableSet.<FunctionDef>of());

    ImmutableMap<String, TableDef> tableDefs = schemaBuilder.getTableDefs();
    assertEquals(2, tableDefs.size());
    TableDef tableDef = tableDefs.get("instant_record");
    assertEquals("instant_record", tableDef.getName());

    ImmutableMap<String, ColumnDef> columnDefs = tableDef.getColumnDefs();
    assertEquals(5, columnDefs.size());

    assertSame(SystemColumnDefs.AUTO_NUMBERED_ID, columnDefs.get("sys_id"));
    assertSame(SystemColumnDefs.TYPE, columnDefs.get("sys_type"));
    assertSame(SystemColumnDefs.INSTANT, columnDefs.get("sys_instant"));
    assertSame(SystemColumnDefs.CONTAINER_ID, columnDefs.get("sys_container_id"));

    assertContainsColumn(columnDefs, "usr_big_decimal", Types.BIGINT);
  }

  @Test
  public void getTableDefsForIntervalRecord() {
    ImmutableSet<RecordType> recordTypes = ImmutableSet.<RecordType>of(
        TestSimpleRecord.TYPE,
        TestIntervalRecord.TYPE);
    SchemaBuilder schemaBuilder = new SchemaBuilder(recordTypes, ImmutableSet.<FunctionDef>of());

    ImmutableMap<String, TableDef> tableDefs = schemaBuilder.getTableDefs();
    assertEquals(2, tableDefs.size());
    TableDef tableDef = tableDefs.get("interval_record");
    assertEquals("interval_record", tableDef.getName());

    ImmutableMap<String, ColumnDef> columnDefs = tableDef.getColumnDefs();
    assertEquals(6, columnDefs.size());

    assertSame(SystemColumnDefs.AUTO_NUMBERED_ID, columnDefs.get("sys_id"));
    assertSame(SystemColumnDefs.TYPE, columnDefs.get("sys_type"));
    assertSame(SystemColumnDefs.START_TIME, columnDefs.get("sys_start_time"));
    assertSame(SystemColumnDefs.END_TIME, columnDefs.get("sys_end_time"));
    assertSame(SystemColumnDefs.CONTAINER_ID, columnDefs.get("sys_container_id"));

    assertContainsColumn(columnDefs, "usr_big_decimal", Types.BIGINT);
  }

  private void assertContainsColumn(ImmutableMap<String, ColumnDef> columnDefs,
      String columnName, int type) {
    ColumnDef columnDef = columnDefs.get(columnName);
    assertEquals(columnName, columnDef.getName());
    assertEquals(type, columnDef.getType());
    // No user-defined columns are required at the database level.
    assertFalse(columnDef.isRequired());
  }

  private interface TestSimpleRecord extends Record<TestSimpleRecord> {
    final RecordType<TestSimpleRecord> TYPE =
        RecordType.builder("simple_record", TestSimpleRecord.class)
            .withManualNumbering()
            .build();

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

  public enum CustomEnum implements Identifiable {
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
    };

    private static final IdentifiableSource IDENTIFIABLE_SOURCE =
        new EnumIdentifiableSource<>(CustomEnum.class);

    /**
     * Supports the Identifiable type contract.
     */
    public static IdentifiableSource getIdentifiableSource() {
      return IDENTIFIABLE_SOURCE;
    }
  }

  private interface TestRootRecord<T extends TestRootRecord> extends Record<T> {
    final RecordType<TestRootRecord> TYPE =
        RecordType.builder("complex_record", TestRootRecord.class)
            .build();

    @AttributeMethod(attribute = "name", required = true)
    String getName();
  }

  private interface TestSubRecord1<T extends TestSubRecord1> extends TestRootRecord<T> {
    final RecordType<TestSubRecord1> TYPE =
        RecordType.builder("sub1", TestSubRecord1.class)
            .extensionOf(TestRootRecord.TYPE)
            .build();

    @AttributeMethod(attribute = "string")
    String getString();
  }

  private interface TestSubRecord11 extends TestSubRecord1<TestSubRecord11> {
    final RecordType<TestSubRecord11> TYPE =
        RecordType.builder("sub11", TestSubRecord11.class)
            .extensionOf(TestSubRecord1.TYPE)
            .build();

    @AttributeMethod(attribute = "other_string")
    String getOtherString();
  }

  private interface TestSubRecord2 extends TestRootRecord<TestSubRecord2> {
    final RecordType<TestSubRecord2> TYPE =
        RecordType.builder("sub2", TestSubRecord2.class)
            .extensionOf(TestRootRecord.TYPE)
            .build();

    @AttributeMethod(attribute = "string")
    String getString();
  }

  private interface TestInstantRecord extends InstantRecord<TestInstantRecord> {
    final RecordType<TestInstantRecord> TYPE =
        RecordType.builder("instant_record", TestInstantRecord.class)
            .containedBy(TestSimpleRecord.TYPE)
            .build();

    @ParentIdMethod
    int getSimpleRecordId();

    @AttributeMethod(attribute = "big_decimal")
    BigDecimal getBigDecimal();
  }

  private interface TestIntervalRecord extends IntervalRecord<TestIntervalRecord> {
    final RecordType<TestIntervalRecord> TYPE =
        RecordType.builder("interval_record", TestIntervalRecord.class)
            .containedBy(TestSimpleRecord.TYPE)
            .build();

    @ParentIdMethod
    int getSimpleRecordId();

    @AttributeMethod(attribute = "big_decimal")
    BigDecimal getBigDecimal();
  }
}
