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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ReportRow {
  private final ImmutableMap<String, Object> dimensions;
  private final List<ReportEntry> entries = new ArrayList<>();

  public ReportRow(Map<String, Object> dimensions) {
    this.dimensions = ImmutableMap.copyOf(dimensions);
  }

  public Object getDimension(String attribute) {
    return dimensions.get(attribute);
  }

  public Iterable<ReportEntry> getEntries() {
    return entries;
  }

  void addEntry(ReportEntry reportEntry) {
    entries.add(reportEntry);
  }
}
