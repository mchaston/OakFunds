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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.chaston.oakfunds.util.DateUtil;
import org.joda.time.Instant;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ReportBuilder {

  private final Map<Map<String, Object>, DimensionAggregator> dimensionAggregators =
      new HashMap<>();
  private final StoreImpl store;
  private final int startYear;
  private final int endYear;
  private final ReportDateGranularity granularity;
  private final String containerIdDimension;
  private final List<RecordSource> recordSources = new ArrayList<>();
  private ImmutableSet<String> measures;
  private final Set<Instant> instants;

  ReportBuilder(StoreImpl store, int startYear, int endYear, ReportDateGranularity granularity,
      @Nullable String containerIdDimension) {
    this.store = store;
    this.startYear = startYear;
    this.endYear = endYear;
    this.granularity = granularity;
    this.containerIdDimension = containerIdDimension;
    instants = buildReportInstants(granularity, startYear, endYear);
  }

  private Set<Instant> buildReportInstants(ReportDateGranularity granularity,
      int startYear, int endYear) {
    ImmutableSet.Builder<Instant> instants = ImmutableSet.builder();
    instants.add(DateUtil.endOfYear(startYear - 1));
    switch (granularity) {
      case YEAR:
        for (int year = startYear; year < endYear; year++) {
          instants.add(DateUtil.endOfYear(year));
        }
        break;
      case MONTH:
        for (int year = startYear; year < endYear; year++) {
          for (int monthOfYear = 1; monthOfYear <= 12; monthOfYear++) {
            instants.add(DateUtil.endOfMonth(year, monthOfYear));
          }
        }
        break;
      default:
        throw new UnsupportedOperationException(
            "Reports of granularity " + granularity + " are not supported.");
    }
    return instants.build();
  }

  public Report build() throws StorageException {
    Set<String> measures = new HashSet<>();
    for (RecordSource<?> recordSource : recordSources) {
      measures.addAll(recordSource.measureMappings.values());
    }
    this.measures = ImmutableSet.copyOf(measures);

    // Run the queries and get the results populated.
    for (RecordSource recordSource : recordSources) {
      store.buildReportPiece(startYear, endYear, granularity, recordSource);
    }

    Report report = new Report();
    for (DimensionAggregator dimensionAggregator : dimensionAggregators.values()) {
      report.addReportRow(dimensionAggregator.createRow());
    }
    return report;
  }

  public <T extends InstantRecord> ReportBuilder addRecordSource(RecordType<T> recordType,
      ImmutableList<? extends SearchTerm> searchTerms,
      ImmutableMap<String, String> dimensionMappings,
      ImmutableMap<String, String> measureMappings) {
    recordSources.add(new RecordSource<>(
        recordType, searchTerms, dimensionMappings, measureMappings));
    return this;
  }

  class RecordSource<T extends InstantRecord> {
    private final RecordType<T> recordType;
    private final ImmutableList<? extends SearchTerm> searchTerms;
    private final ImmutableMap<String, String> dimensionMappings;
    private final ImmutableMap<String, String> measureMappings;

    RecordSource(RecordType<T> recordType,
        ImmutableList<? extends SearchTerm> searchTerms,
        ImmutableMap<String, String> dimensionMappings,
        ImmutableMap<String, String> measureMappings) {
      this.recordType = recordType;
      this.searchTerms = searchTerms;
      this.dimensionMappings = dimensionMappings;
      this.measureMappings = measureMappings;
    }

    RecordType<T> getRecordType() {
      return recordType;
    }

    ImmutableList<? extends SearchTerm> getSearchTerms() {
      return searchTerms;
    }

    Set<String> getDimensionAttributes() {
      return dimensionMappings.keySet();
    }

    Set<String> getMeasureAttributes() {
      return measureMappings.keySet();
    }

    void aggregateEntry(Instant instant, int containerId, Map<String, Object> value) {
      ImmutableMap<String, Object> dimensionValues = readDimensionValues(containerId, value);
      DimensionAggregator dimensionAggregator = dimensionAggregators.get(dimensionValues);
      if (dimensionAggregator == null) {
        dimensionAggregator = new DimensionAggregator(dimensionValues, instants);
        dimensionAggregators.put(dimensionValues, dimensionAggregator);
      }
      dimensionAggregator.aggregateMeasures(instant, readMeasureValues(value));
    }

    private ImmutableMap<String, Object> readDimensionValues(
        int containerId, Map<String, Object> value) {
      ImmutableMap.Builder<String, Object> dimensionValues = ImmutableMap.builder();
      for (Map.Entry<String, String> dimensionMapping : dimensionMappings.entrySet()) {
        Object dimensionValue = value.get(dimensionMapping.getKey());
        if (dimensionValue != null) {
          dimensionValues.put(dimensionMapping.getValue(), dimensionValue);
        }
      }
      if (containerIdDimension != null) {
        dimensionValues.put(containerIdDimension, containerId);
      }
      return dimensionValues.build();
    }

    private Map<String, BigDecimal> readMeasureValues(Map<String, Object> value) {
      ImmutableMap.Builder<String, BigDecimal> measureValues = ImmutableMap.builder();
      for (Map.Entry<String, String> measureMapping : measureMappings.entrySet()) {
        BigDecimal measureValue = (BigDecimal) value.get(measureMapping.getKey());
        if (measureValue == null) {
          measureValues.put(measureMapping.getValue(), BigDecimal.ZERO);
        } else {
          measureValues.put(measureMapping.getValue(), measureValue);
        }
      }
      return measureValues.build();
    }
  }

  private class DimensionAggregator {

    private final ImmutableMap<String, Object> dimensionValues;
    private final SortedMap<Instant, MeasureAggregator> measureAggregators = new TreeMap<>();

    DimensionAggregator(ImmutableMap<String, Object> dimensionValues, Set<Instant> instants) {
      this.dimensionValues = dimensionValues;
      for (Instant instant : instants) {
        measureAggregators.put(instant, new MeasureAggregator(instant));
      }
    }

    public void aggregateMeasures(Instant instant, Map<String, BigDecimal> measureValues) {
      SortedMap<Instant, MeasureAggregator> tailMap = measureAggregators.tailMap(instant);
      if (!tailMap.isEmpty()) {
        tailMap.get(tailMap.firstKey()).aggregateMeasures(measureValues);
      }
    }

    public ReportRow createRow() {
      ReportRow reportRow = new ReportRow(dimensionValues);
      Map<String, BigDecimal> measureValues = null;
      for (MeasureAggregator measureAggregator : measureAggregators.values()) {
        if (measureValues == null) {
          measureValues = new HashMap<>(measureAggregator.getMeasureValues());
        } else {
          addAllMeasures(measureValues, measureAggregator.getMeasureValues());
        }
        reportRow.addEntry(
            new ReportEntry(measureAggregator.getInstant(), ImmutableMap.copyOf(measureValues)));
      }
      return reportRow;
    }
  }

  private class MeasureAggregator {
    private final Instant instant;
    private final Map<String, BigDecimal> measureValues = new HashMap<>();

    MeasureAggregator(Instant instant) {
      this.instant = instant;
      for (String measure : ReportBuilder.this.measures) {
        measureValues.put(measure, BigDecimal.ZERO);
      }
    }

    void aggregateMeasures(Map<String, BigDecimal> measureValues) {
      addAllMeasures(this.measureValues, measureValues);
    }

    public Instant getInstant() {
      return instant;
    }

    public Map<String,BigDecimal> getMeasureValues() {
      return measureValues;
    }
  }

  private static void addAllMeasures(Map<String, BigDecimal> currentMeasures,
      Map<String, BigDecimal> additionalMeasures) {
    for (Map.Entry<String, BigDecimal> entry : additionalMeasures.entrySet()) {
      String attribute = entry.getKey();
      currentMeasures.put(attribute, currentMeasures.get(attribute).add(entry.getValue()));
    }
  }
}
