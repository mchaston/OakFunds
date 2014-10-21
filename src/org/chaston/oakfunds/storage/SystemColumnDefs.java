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

import java.sql.Types;

/**
 * TODO(mchaston): write JavaDocs
 */
public class SystemColumnDefs {
  public static final String SCHEMA = "oakfunds";

  public static final String SYSTEM_COLUMN_PREFIX = "sys_";
  public static final String USER_COLUMN_PREFIX = "usr_";

  public static final String ID_COLUMN_NAME =
      SYSTEM_COLUMN_PREFIX + "id";

  public static final ColumnDef MANUAL_ID =
      new ColumnDef(ID_COLUMN_NAME, Types.INTEGER, true, false);
  public static final ColumnDef AUTO_NUMBERED_ID =
      new ColumnDef(ID_COLUMN_NAME, Types.INTEGER, true, true);
  public static final ColumnDef TYPE =
      new ColumnDef(SYSTEM_COLUMN_PREFIX + "type", Types.VARCHAR, true);
  public static final ColumnDef CONTAINER_ID =
      new ColumnDef(SYSTEM_COLUMN_PREFIX + "container_id", Types.INTEGER, true);
  public static final ColumnDef INSTANT =
      new ColumnDef(SYSTEM_COLUMN_PREFIX + "instant", Types.TIMESTAMP, true);
  public static final ColumnDef START_TIME =
      new ColumnDef(SYSTEM_COLUMN_PREFIX + "start_time", Types.TIMESTAMP, true);
  public static final ColumnDef END_TIME =
      new ColumnDef(SYSTEM_COLUMN_PREFIX + "end_time", Types.TIMESTAMP, true);
}
