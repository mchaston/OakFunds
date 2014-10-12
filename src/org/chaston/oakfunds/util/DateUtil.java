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

import org.joda.time.DurationFieldType;
import org.joda.time.Instant;
import org.joda.time.MutableDateTime;

/**
 * TODO(mchaston): write JavaDocs
 */
public class DateUtil {
  public static final Instant BEGINNING_OF_TIME = Instant.parse("2000-01-01T00:00:00");
  public static final Instant END_OF_TIME = Instant.parse("2101-01-01T00:00:00");

  public static Instant endOfYear(int year) {
    MutableDateTime mutableDateTime = new MutableDateTime(year, 1, 1, 0, 0, 0, 0);
    mutableDateTime.add(DurationFieldType.years(), 1);
    mutableDateTime.add(DurationFieldType.millis(), -1);
    return mutableDateTime.toInstant();
  }

  public static Instant endOfMonth(int year, int monthOfYear) {
    MutableDateTime mutableDateTime = new MutableDateTime(year, monthOfYear, 1, 0, 0, 0, 0);
    mutableDateTime.add(DurationFieldType.months(), 1);
    mutableDateTime.add(DurationFieldType.millis(), -1);
    return mutableDateTime.toInstant();
  }

  public static Instant endOfDay(int year, int monthOfYear, int dayOfMonth) {
    MutableDateTime mutableDateTime =
        new MutableDateTime(year, monthOfYear, dayOfMonth, 0, 0, 0, 0);
    mutableDateTime.add(DurationFieldType.days(), 1);
    mutableDateTime.add(DurationFieldType.millis(), -1);
    return mutableDateTime.toInstant();
  }
}
