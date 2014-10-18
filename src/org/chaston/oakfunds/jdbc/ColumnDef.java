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

/**
 * TODO(mchaston): write JavaDocs
 */
public class ColumnDef {
  private final String name;
  private final int type;
  private final boolean required;
  private final boolean autoNumbered;

  public ColumnDef(String name, int type, boolean required, boolean autoNumbered) {
    this.name = name;
    this.type = type;
    this.required = required;
    this.autoNumbered = autoNumbered;
  }

  public ColumnDef(String name, int type, boolean required) {
    this(name, type, required, false);
  }

  public String getName() {
    return name;
  }

  public int getType() {
    return type;
  }

  public boolean isRequired() {
    return required;
  }

  public boolean isAutoNumbered() {
    return autoNumbered;
  }
}
