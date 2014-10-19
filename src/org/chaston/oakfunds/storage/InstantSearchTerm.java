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

import org.chaston.oakfunds.jdbc.ColumnDef;
import org.joda.time.Instant;

/**
 * TODO(mchaston): write JavaDocs
 */
public class InstantSearchTerm extends SearchTerm {
  private final ColumnDef columnDef;
  private final SearchOperator operator;
  private final Instant instant;

  InstantSearchTerm(ColumnDef columnDef, SearchOperator operator, Instant instant) {
    this.columnDef = columnDef;
    this.operator = operator;

    this.instant = instant;
  }

  public static InstantSearchTerm of(ColumnDef columnDef, SearchOperator operator,
      Instant instant) {
    return new InstantSearchTerm(columnDef, operator, instant);
  }

  public SearchOperator getOperator() {
    return operator;
  }

  public Instant getInstant() {
    return instant;
  }

  public ColumnDef getColumnDef() {
    return columnDef;
  }
}
