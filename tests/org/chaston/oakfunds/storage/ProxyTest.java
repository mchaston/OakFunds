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
import org.chaston.oakfunds.util.BigDecimalUtil;
import org.joda.time.Instant;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class ProxyTest {
  @Test
  public void recordProxy() {
    ImmutableMap.Builder<String, Object> attributes = ImmutableMap.builder();
    attributes.put("name", "a name");
    attributes.put("string", "a string");
    attributes.put("boolean", true);
    attributes.put("int", 4);
    attributes.put("date", Instant.parse("2014-4-25T10:30:12"));
    attributes.put("big_decimal", BigDecimalUtil.valueOf(12.34567));
    attributes.put("identifiable", CustomEnum.SECOND);

    TestSimpleRecord record =
        RecordProxy.proxyRecord(TestSimpleRecord.TYPE, null, 1, attributes.build());

    JSONObject jsonObject = record.toJSONObject();
    assertNotNull(jsonObject);
    assertEquals(1, jsonObject.get("id"));
    assertEquals("simple_record", jsonObject.get("type"));

    JSONObject jsonAttributes = (JSONObject) jsonObject.get("attributes");
    assertNotNull(jsonAttributes);
    assertEquals("a name", jsonAttributes.get("name"));
    assertEquals("a string", jsonAttributes.get("string"));
    assertEquals(true, jsonAttributes.get("boolean"));
    assertEquals(4, jsonAttributes.get("int"));
    assertEquals("2014-04-25T17:30:12.000Z", jsonAttributes.get("date"));
    assertEquals("12.34567", jsonAttributes.get("big_decimal"));
    assertEquals("second", jsonAttributes.get("identifiable"));
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

      @Override
      public String toJson() {
        return "first";
      }
    }, SECOND {
      @Override
      public byte identifier() {
        return 2;
      }

      @Override
      public String toJson() {
        return "second";
      }
    }, THIRD {
      @Override
      public byte identifier() {
        return 3;
      }

      @Override
      public String toJson() {
        return "third";
      }
    };

    private static final IdentifiableSource<CustomEnum> IDENTIFIABLE_SOURCE =
        new EnumIdentifiableSource<>(CustomEnum.class);

    /**
     * Supports the Identifiable type contract.
     */
    public static IdentifiableSource<CustomEnum> getIdentifiableSource() {
      return IDENTIFIABLE_SOURCE;
    }
  }
}
