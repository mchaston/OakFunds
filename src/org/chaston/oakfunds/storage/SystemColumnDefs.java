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
  public static final String ID_COLUMN_NAME = "sys_id";
  public static final ColumnDef MANUAL_ID = new ColumnDef(ID_COLUMN_NAME, Types.INTEGER, true, false);
  public static final ColumnDef AUTO_NUMBERED_ID = new ColumnDef(ID_COLUMN_NAME, Types.INTEGER, true, true);
  public static final ColumnDef TYPE = new ColumnDef("sys_type", Types.VARCHAR, true);
  public static final ColumnDef CONTAINER_ID = new ColumnDef("sys_container_id", Types.INTEGER, true);
  public static final ColumnDef INSTANT = new ColumnDef("sys_instant", Types.TIMESTAMP, true);
  public static final ColumnDef START_TIME = new ColumnDef("sys_start_time", Types.TIMESTAMP, true);
  public static final ColumnDef END_TIME = new ColumnDef("sys_end_time", Types.TIMESTAMP, true);
}
