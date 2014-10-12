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
import org.joda.time.Instant;

import java.math.BigDecimal;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ReportEntry {
  private final Instant instant;
  private final ImmutableMap<String, BigDecimal> attributes;

  ReportEntry(Instant instant,
      Map<String, BigDecimal> measures) {
    this.instant = instant;
    this.attributes = ImmutableMap.copyOf(measures);
  }

  public Instant getInstant() {
    return instant;
  }

  public BigDecimal getMeasure(String attribute) {
    return attributes.get(attribute);
  }
}
