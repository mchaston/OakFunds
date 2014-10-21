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

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import org.chaston.oakfunds.jdbc.FunctionDef;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * TODO(mchaston): write JavaDocs
 */
public class StorageTypesModule extends AbstractModule {

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  private @interface ReportingYearFunction{}

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  private @interface ReportingMonthFunction{}

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  private @interface ReportingDayFunction{}

  @Override
  protected void configure() {
    Multibinder<FunctionDef> functionDefMultibinder
        = Multibinder.newSetBinder(binder(), FunctionDef.class);
    functionDefMultibinder.addBinding()
        .to(Key.get(FunctionDef.class, ReportingYearFunction.class));
    functionDefMultibinder.addBinding()
        .to(Key.get(FunctionDef.class, ReportingMonthFunction.class));
    functionDefMultibinder.addBinding()
        .to(Key.get(FunctionDef.class, ReportingDayFunction.class));
  }

  @Provides
  @ReportingYearFunction
  private FunctionDef provideReportingYearFunctionDef() throws IOException {
    return createFunctionDef("reporting_year");
  }

  @Provides
  @ReportingMonthFunction
  private FunctionDef provideReportingMonthFunctionDef() throws IOException {
    return createFunctionDef("reporting_month");
  }

  @Provides
  @ReportingDayFunction
  private FunctionDef provideReportingDayFunctionDef() throws IOException {
    return createFunctionDef("reporting_day");
  }

  private FunctionDef createFunctionDef(String functionName) throws IOException {
    return new FunctionDef(SystemColumnDefs.SCHEMA, functionName,
        "org/chaston/oakfunds/storage/" + functionName + "_hsqldb.sql",
        "org/chaston/oakfunds/storage/" + functionName + "_mysql.sql");
  }
}
