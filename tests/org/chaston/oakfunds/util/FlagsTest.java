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
package org.chaston.oakfunds.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class FlagsTest {

  private static final Flag<Integer> integerFlag =
      Flag.builder("integerFlag", 23)
          .setShortName("i")
          .build();

  private static final Flag<String> stringFlag =
      Flag.builder("stringFlag", "foo")
          .setShortName("s")
          .build();

  private static final Flag<Boolean> booleanFlag =
      Flag.builder("booleanFlag", false)
          .setShortName("b")
          .build();

  private static final Flag<CustomValue> customFlag1 =
      Flag.builder("customValueFlag1", new CustomValueFlagValueParser())
          .build();

  private static final Flag<CustomValue> customFlag2 =
      Flag.builder("customValueFlag2", new CustomValueFlagValueParser(), new CustomValue("bar"))
          .build();

  @Test
  public void readSimpleFlagDefaults() {
    assertEquals((Integer) 23, integerFlag.get());
    assertEquals("foo", stringFlag.get());
    assertEquals(false, booleanFlag.get());
  }

  @Test
  public void hasRemainingArgs() {
    String[] remainingArgs =
        Flags.parse(new String[] {
            "v1",
            "--integerFlag=55",
            "--stringFlag=bar",
            "v2",
            "--booleanFlag=true",
            "v3"});
    assertEquals(3, remainingArgs.length);
    assertEquals("v1", remainingArgs[0]);
    assertEquals("v2", remainingArgs[1]);
    assertEquals("v3", remainingArgs[2]);

    assertEquals((Integer) 55, integerFlag.get());
    assertEquals("bar", stringFlag.get());
    assertEquals(true, booleanFlag.get());
  }

  @Test
  public void readSimpleFlagParsed() {
    String[] remainingArgs =
        Flags.parse(new String[] {
            "--integerFlag=55",
            "--stringFlag=bar",
            "--booleanFlag=true"});
    assertEquals(0, remainingArgs.length);

    assertEquals((Integer) 55, integerFlag.get());
    assertEquals("bar", stringFlag.get());
    assertEquals(true, booleanFlag.get());
  }

  @Test
  public void readSimpleFlagParsedWithShortNames() {
    String[] remainingArgs =
        Flags.parse(new String[] {
            "-i=55",
            "-s=bar",
            "-b=true"});
    assertEquals(0, remainingArgs.length);

    assertEquals((Integer) 55, integerFlag.get());
    assertEquals("bar", stringFlag.get());
    assertEquals(true, booleanFlag.get());
  }

  @Test
  public void readCustomFlagValue() {
    assertNull(customFlag1.get());
    assertEquals("bar", customFlag2.get().getValue());

    String[] remainingArgs =
        Flags.parse(new String[]{
            "--customValueFlag1=foo",
            "--customValueFlag2=baz"});
    assertEquals(0, remainingArgs.length);

    assertEquals("foo", customFlag1.get().getValue());
    assertEquals("baz", customFlag2.get().getValue());
  }

  private static class CustomValue {
    private final String value;

    public CustomValue(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private static class CustomValueFlagValueParser implements FlagValueParser<CustomValue> {
    @Override
    public CustomValue parse(String value) {
      return new CustomValue(value);
    }
  }
}
