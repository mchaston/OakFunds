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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * TODO(mchaston): write JavaDocs
 */
class IdentifiableTypeHandler extends JdbcTypeHandler {

  private final IdentifiableSource identifiableSource;

  public IdentifiableTypeHandler(String columnName, Class<? extends Identifiable> type) {
    super(columnName);
    try {
      identifiableSource =
          (IdentifiableSource) type.getMethod("getIdentifiableSource").invoke(null);
    } catch (Exception e) {
      throw new NoSuchMethodError("The " + type.getName()
          + " class did not implement the required static getIdentifiableSource method.");
    }
  }

  @Override
  Object get(ResultSet rs) throws SQLException {
    byte value = rs.getByte(getColumnName());
    return rs.wasNull() ? null : identifiableSource.lookup(value);
  }

  @Override
  void set(PreparedStatement stmt, int index, Object value) throws SQLException {
    stmt.setByte(index, ((Identifiable) value).identifier());
  }

  @Override
  Object toJson(Object value) {
    return ((Identifiable) value).toJson();
  }
}
