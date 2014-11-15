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
package org.chaston.oakfunds.servlet;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.chaston.oakfunds.jdbc.LocalDataSourceModule;
import org.chaston.oakfunds.security.LocalUserAuthenticatorModule;
import org.chaston.oakfunds.storage.mgmt.SchemaDeploymentTask;
import org.chaston.oakfunds.system.TestSystemBootstrapModuleBuilder;

/**
 * TODO(mchaston): write JavaDocs
 */
@SuppressWarnings("UnusedDeclaration") // Used via reflection.
public class DevEnvironmentModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new LocalDataSourceModule());
    install(new LocalUserAuthenticatorModule());
    install(new TestSystemBootstrapModuleBuilder()
        .setCurrentYear(2014)
        .setTimeHorizon(15)
        .build());

    bind(SchemaDeploymentTask.class).in(Singleton.class);
  }
}
