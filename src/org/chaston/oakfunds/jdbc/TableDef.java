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
package org.chaston.oakfunds.jdbc;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class TableDef {
  private final String schema;
  private final String name;
  private final ImmutableMap<String, ColumnDef> columnDefs;

  private TableDef(String schema, String name, ImmutableMap<String, ColumnDef> columnDefs) {
    this.schema = schema;
    this.name = name;
    this.columnDefs = columnDefs;
  }

  public static Builder builder(String schema, String name) {
    return new Builder(schema, name);
  }

  public String getFullName() {
    return schema + "." + name;
  }

  public String getName() {
    return name;
  }

  public ImmutableMap<String, ColumnDef> getColumnDefs() {
    return columnDefs;
  }

  public static class Builder {
    private final String schema;
    private final String name;
    private final Map<String, ColumnDef> columnDefs = new HashMap<>();

    private Builder(String schema, String name) {
      this.schema = schema;
      this.name = name;
    }

    public Builder addColumnDef(ColumnDef columnDef) {
      if (columnDefs.containsKey(columnDef.getName())) {
        throw new IllegalStateException(
            "TableDef already contains column of name " + columnDef.getName() + ".");
      }
      columnDefs.put(columnDef.getName(), columnDef);
      return this;
    }

    public TableDef build() {
      return new TableDef(schema, name, ImmutableMap.<String, ColumnDef>of().copyOf(columnDefs));
    }
  }
}
