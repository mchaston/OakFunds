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

import org.chaston.oakfunds.util.BigDecimalUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * TODO(mchaston): write JavaDocs
 */
class BigDecimalTypeHandler extends JdbcTypeHandler {
  private static final int SCALE = BigDecimalUtil.STANDARD_SCALE;

  public BigDecimalTypeHandler(String attribute) {
    super(attribute);
  }

  @Override
  Object get(ResultSet rs) throws SQLException {
    long value = rs.getLong(getAttribute());
    return rs.wasNull() ? null : BigDecimal.valueOf(value, SCALE);
  }

  @Override
  void set(PreparedStatement stmt, int index, Object value) throws SQLException {
    BigDecimal bigDecimal = (BigDecimal) value;
    if (bigDecimal.scale() != SCALE) {
      bigDecimal = bigDecimal.setScale(SCALE, RoundingMode.HALF_UP);
    }
    stmt.setLong(index, bigDecimal.unscaledValue().longValue());
  }
}
