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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * TODO(mchaston): write JavaDocs
 */
class ReportBuilder {

  private final Map<Map<String, Object>, DimensionAggregator> dimensionAggregators =
      new HashMap<>();
  @Nullable
  private final String parentIdDimension;
  private final ImmutableList<String> dimensions;
  private final ImmutableList<String> measures;
  private final Set<Instant> instants;

  public ReportBuilder(ReportDateGranularity granularity, int startYear, int endYear,
      @Nullable String parentIdDimension, List<String> dimensions, List<String> measures) {
    this.parentIdDimension = parentIdDimension;
    this.dimensions = ImmutableList.copyOf(dimensions);
    this.measures = ImmutableList.copyOf(measures);
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

  public void aggregateEntry(Instant instant, int parentId, Map<String, Object> value) {
    ImmutableMap<String, Object> dimensionValues = readDimensionValues(parentId, value);
    DimensionAggregator dimensionAggregator = dimensionAggregators.get(dimensionValues);
    if (dimensionAggregator == null) {
      dimensionAggregator = new DimensionAggregator(dimensionValues, instants);
      dimensionAggregators.put(dimensionValues, dimensionAggregator);
    }
    dimensionAggregator.aggregateMeasures(instant, readMeasureValues(value));
  }

  private ImmutableMap<String, Object> readDimensionValues(
      int parentId, Map<String, Object> value) {
    ImmutableMap.Builder<String, Object> dimensionValues = ImmutableMap.builder();
    for (String dimension : dimensions) {
      Object dimensionValue = value.get(dimension);
      if (dimensionValue != null) {
        dimensionValues.put(dimension, dimensionValue);
      }
    }
    if (parentIdDimension != null) {
      dimensionValues.put(parentIdDimension, parentId);
    }
    return dimensionValues.build();
  }

  private Map<String, BigDecimal> readMeasureValues(Map<String, Object> value) {
    ImmutableMap.Builder<String, BigDecimal> measureValues = ImmutableMap.builder();
    for (String measure : measures) {
      BigDecimal measureValue = (BigDecimal) value.get(measure);
      if (measureValue == null) {
        measureValues.put(measure, BigDecimal.ZERO);
      } else {
        measureValues.put(measure, measureValue);
      }
    }
    return measureValues.build();
  }

  public Report build() {
    Report report = new Report();
    for (DimensionAggregator dimensionAggregator : dimensionAggregators.values()) {
      report.addReportRow(dimensionAggregator.createRow());
    }
    return report;
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
