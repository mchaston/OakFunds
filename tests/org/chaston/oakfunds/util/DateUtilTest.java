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

import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.TestCase.assertEquals;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class DateUtilTest {
  @Test
  public void endOfYear() {
    assertEquals(Instant.parse("2015-01-01").minus(1), DateUtil.endOfYear(2014));
  }

  @Test
  public void endOfMonth() {
    assertEquals(Instant.parse("2015-01-01").minus(1), DateUtil.endOfMonth(2014, 12));
    assertEquals(Instant.parse("2014-05-01").minus(1), DateUtil.endOfMonth(2014, 4));
    assertEquals(Instant.parse("2014-02-01").minus(1), DateUtil.endOfMonth(2014, 1));
  }

  @Test
  public void endOfDay() {
    assertEquals(Instant.parse("2015-01-01").minus(1), DateUtil.endOfDay(2014, 12, 31));
    assertEquals(Instant.parse("2014-04-06").minus(1), DateUtil.endOfDay(2014, 4, 5));
    assertEquals(Instant.parse("2014-02-01").minus(1), DateUtil.endOfDay(2014, 1, 31));
  }
}
