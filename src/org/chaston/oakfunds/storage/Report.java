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

/**
 * TODO(mchaston): write JavaDocs
 */
public class Report {
  private final List<ReportRow> rows = new ArrayList<>();

  public Iterable<ReportRow> getRows() {
    return rows;
  }

  void addReportRow(ReportRow reportRow) {
    rows.add(reportRow);
  }

  public ReportRow getRow(ImmutableMap<String, Object> dimensions) {
    for (ReportRow row : rows) {
      if (row.getDimensions().equals(dimensions)) {
        return row;
      }
    }
    return null;
  }
}
