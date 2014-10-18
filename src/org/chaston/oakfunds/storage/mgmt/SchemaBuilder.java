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
package org.chaston.oakfunds.storage.mgmt;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.inject.Inject;
import org.chaston.oakfunds.jdbc.ColumnDef;
import org.chaston.oakfunds.jdbc.TableDef;
import org.chaston.oakfunds.storage.AttributeType;
import org.chaston.oakfunds.storage.Identifiable;
import org.chaston.oakfunds.storage.RecordType;
import org.chaston.oakfunds.storage.SystemColumnDefs;
import org.joda.time.Instant;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * TODO(mchaston): write JavaDocs
 */
public class SchemaBuilder {
  private final ImmutableMap<String, TableDef> tableDefs;

  @Inject
  SchemaBuilder(Set<RecordType> recordTypes) {
    Multimap<RecordType, RecordType> typesBySuperType = groupBySuperType(recordTypes);
    Map<String, TableDef.Builder> tableDefBuilders = createBaseTables(typesBySuperType);
    tableDefs = ImmutableMap.copyOf(Maps.transformValues(tableDefBuilders,
        new Function<TableDef.Builder, TableDef>() {
          @Override
          public TableDef apply(TableDef.Builder builder) {
            return builder.build();
          }
        }));
  }

  private Multimap<RecordType, RecordType> groupBySuperType(Set<? extends RecordType> recordTypes) {
    Multimap<RecordType, RecordType> typesBySuperType =
        MultimapBuilder.hashKeys().arrayListValues().build();
    for (RecordType recordType : recordTypes) {
      typesBySuperType.put(recordType.getRootType(), recordType);
    }
    return typesBySuperType;
  }

  private Map<String, TableDef.Builder> createBaseTables(
      Multimap<RecordType, RecordType> typesBySuperType) {
    ImmutableMap.Builder<String, TableDef.Builder> baseTables = ImmutableMap.builder();
    for (Map.Entry<RecordType, Collection<RecordType>> entry
        : typesBySuperType.asMap().entrySet()) {
      RecordType rootType = entry.getKey();
      TableDef.Builder tableDefBuilder = TableDef.builder(rootType.getName());
      tableDefBuilder.addColumnDef(SystemColumnDefs.ID);
      tableDefBuilder.addColumnDef(SystemColumnDefs.TYPE);

      switch (rootType.getTemporalType()) {
        case NONE:
          // Nothing more to add.
          break;
        case INSTANT:
          tableDefBuilder.addColumnDef(SystemColumnDefs.INSTANT);
          break;
        case INTERVAL:
          tableDefBuilder.addColumnDef(SystemColumnDefs.START_TIME);
          tableDefBuilder.addColumnDef(SystemColumnDefs.END_TIME);
          break;
        default:
          throw new UnsupportedOperationException(
              "Temporal type " + rootType.getTemporalType() + " is not supported.");
      }

      if (rootType.getContainingType() != null) {
        tableDefBuilder.addColumnDef(SystemColumnDefs.PARENT_ID);
      }

      Collection<RecordType> subTypes = entry.getValue();
      for (RecordType<?> subType : subTypes) {
        String columnPrefix = "";
        if (!subType.equals(rootType)) {
          columnPrefix = subType.getName() + "__";
        }
        for (AttributeType attributeType : subType.getAttributes().values()) {
          tableDefBuilder.addColumnDef(new ColumnDef(
              columnPrefix + attributeType.getName(),
              toSqlType(attributeType.getType()),
              attributeType.isRequired()));
        }
      }
      baseTables.put(rootType.getName(), tableDefBuilder);
    }
    return baseTables.build();
  }

  private int toSqlType(Class<?> attributeType) {
    if (String.class.equals(attributeType)) {
      return Types.VARCHAR;
    }
    if (Integer.class.equals(attributeType) || Integer.TYPE.equals(attributeType)) {
      return Types.INTEGER;
    }
    if (Boolean.class.equals(attributeType) || Boolean.TYPE.equals(attributeType)) {
      return Types.BOOLEAN;
    }
    if (BigDecimal.class.equals(attributeType)) {
      return Types.BIGINT;
    }
    if (Instant.class.equals(attributeType)) {
      return Types.TIMESTAMP;
    }
    if (Identifiable.class.isAssignableFrom(attributeType)) {
      return Types.SMALLINT;
    }
    throw new UnsupportedOperationException("Type " + attributeType + " is not supported.");
  }

  public ImmutableMap<String, TableDef> getTableDefs() {
    return tableDefs;
  }
}
