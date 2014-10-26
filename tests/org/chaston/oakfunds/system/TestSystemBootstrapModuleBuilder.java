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
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.chaston.oakfunds.bootstrap.BootstrapTask;
import org.chaston.oakfunds.bootstrap.TransactionalBootstrapTask;
import org.chaston.oakfunds.security.AuthorizationContext;
import org.chaston.oakfunds.security.SinglePermissionAssertion;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.mgmt.SchemaDeploymentTask;

import static com.google.common.base.Preconditions.checkState;

/**
 * TODO(mchaston): write JavaDocs
 */
public class TestSystemBootstrapModuleBuilder {
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
        bind(new TypeLiteral<Iterable<SystemPropertyLoader>>() {})
            .toInstance(ImmutableList.of(
                SystemPropertyLoader.createIntegerProperty(
                    SystemPropertiesManagerImpl.PROPERTY_CURRENT_YEAR, currentYear),
                SystemPropertyLoader.createIntegerProperty(
                    SystemPropertiesManagerImpl.PROPERTY_TIME_HORIZON, timeHorizon)));

        Multibinder<BootstrapTask> bootstrapTaskBinder =
            Multibinder.newSetBinder(binder(), BootstrapTask.class);
        bootstrapTaskBinder.addBinding().to(TestSystemPropertyBootstrapTask.class);
      }
    };
  }

  /**
   * Used for bootstrapping system properties into tests.
   */
  private static class TestSystemPropertyBootstrapTask extends TransactionalBootstrapTask {

    private final AuthorizationContext authorizationContext;
    private final Iterable<SystemPropertyLoader> systemPropertyLoaders;

    @Inject
    TestSystemPropertyBootstrapTask(
        SchemaDeploymentTask schemaDeploymentTask, // Here for dependency enforcement.
        AuthorizationContext authorizationContext,
        Store store,
        Iterable<SystemPropertyLoader> systemPropertyLoaders) throws StorageException {
      super(store);
      this.authorizationContext = authorizationContext;
      this.systemPropertyLoaders = systemPropertyLoaders;
    }

    @Override
    protected void bootstrapDuringTransaction() throws Exception {
      try (SinglePermissionAssertion singlePermissionAssertion =
               authorizationContext.assertPermission("system_property.create")) {
        for (SystemPropertyLoader systemPropertyLoader : systemPropertyLoaders) {
          systemPropertyLoader.load(getStore());
        }
      }
    }
  }
}
