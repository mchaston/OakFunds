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
import java.sql.Types;

/**
 * TODO(mchaston): write JavaDocs
 */
class IntegerTypeHandler extends JdbcTypeHandler {
  public IntegerTypeHandler(String columnName) {
    super(columnName);
  }

  @Override
  Object get(ResultSet rs) throws SQLException {
    int value = rs.getInt(getColumnName());
    return rs.wasNull() ? null : value;
  }

  @Override
  void set(PreparedStatement stmt, int index, Object value) throws SQLException {
    if (value == null) {
      stmt.setNull(index, Types.INTEGER);
    } else {
      stmt.setInt(index, (Integer) value);
    }
  }
}
