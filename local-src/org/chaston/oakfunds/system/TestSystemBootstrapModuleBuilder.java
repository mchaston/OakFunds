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
package org.chaston.oakfunds.system;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import org.chaston.oakfunds.storage.mgmt.SchemaDeploymentTask;
import org.chaston.oakfunds.system.SystemBootstrapModule.IntegerSystemPropertyDef;
import org.chaston.oakfunds.system.SystemBootstrapModule.SystemPropertyDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.google.common.base.Preconditions.checkState;

/**
 * TODO(mchaston): write JavaDocs
 */
public class TestSystemBootstrapModuleBuilder {

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  private @interface CurrentYear {}

  @Retention(RetentionPolicy.RUNTIME)
  @BindingAnnotation
  private @interface TimeHorizon {}

  private Integer currentYear;
  private Integer timeHorizon;

  public TestSystemBootstrapModuleBuilder setCurrentYear(int currentYear) {
    this.currentYear = currentYear;
    return this;
  }

  public TestSystemBootstrapModuleBuilder setTimeHorizon(int timeHorizon) {
    this.timeHorizon = timeHorizon;
    return this;
  }

  public Module build() {
    checkState(currentYear != null, "Current year must be set.");
    checkState(timeHorizon != null, "Time horizon must be set.");

    return new AbstractModule() {
      @Override
      protected void configure() {
        install(new SystemBootstrapModule() {
          @Override
          protected void configure() {
            super.configure();
            bindConstant().annotatedWith(CurrentYear.class).to(currentYear);
            bindConstant().annotatedWith(TimeHorizon.class).to(timeHorizon);
          }

          @Override
          protected Class<? extends Provider<Iterable<SystemPropertyDef>>>
              getSystemPropertyDefsProviderClass() {
            return SystemPropertyDefsProvider.class;
          }
        });
      }
    };
  }

  private static class SystemPropertyDefsProvider
      implements Provider<Iterable<SystemPropertyDef>> {
    private final int currentYear;
    private final int timeHorizon;

    @Inject
    SystemPropertyDefsProvider(
        // Here for dependency enforcement.
        SchemaDeploymentTask schemaDeploymentTask,
        @CurrentYear int currentYear,
        @TimeHorizon int timeHorizon) {
      this.currentYear = currentYear;
      this.timeHorizon = timeHorizon;
    }

    @Override
    public Iterable<SystemPropertyDef> get() {
      return ImmutableList.<SystemPropertyDef>of(
          new IntegerSystemPropertyDef(
              SystemPropertiesManagerImpl.PROPERTY_CURRENT_YEAR, currentYear),
          new IntegerSystemPropertyDef(
              SystemPropertiesManagerImpl.PROPERTY_TIME_HORIZON, timeHorizon));
    }
  }
}
