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

/**
 * TODO(mchaston): write JavaDocs
 */
public class AttributeType {
  private final String name;
  private final Class<?> type;
  private final String columnName;
  private final boolean required;

  AttributeType(String name, Class<?> type, String columnName, boolean required) {
    this.name = name;
    this.type = type;
    this.columnName = columnName;
    this.required = required;
  }

  public String getColumnName() {
    return columnName;
  }

  public String getName() {
    return name;
  }

  public Class<?> getType() {
    return type;
  }

  public boolean isRequired() {
    return required;
  }
}
