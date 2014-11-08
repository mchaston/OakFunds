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

import com.google.appengine.api.utils.SystemProperty;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.util.Modules;
import org.chaston.oakfunds.account.AccountCodeModule;
import org.chaston.oakfunds.appengine.AppEngineModule;
import org.chaston.oakfunds.bootstrap.BootstrapModule;
import org.chaston.oakfunds.gitkit.GitKitUserAuthenticatorModule;
import org.chaston.oakfunds.jdbc.AppEngineDataStoreModule;
import org.chaston.oakfunds.ledger.LedgerModule;
import org.chaston.oakfunds.model.ModelModule;
import org.chaston.oakfunds.security.UserSecurityModule;
import org.chaston.oakfunds.storage.RecordTypeRegistryModule;
import org.chaston.oakfunds.storage.StorageModule;
import org.chaston.oakfunds.system.SystemModule;
import org.chaston.oakfunds.xsrf.XsrfModule;

/**
 * TODO(mchaston): write JavaDocs
 */
public class AppContextListener extends GuiceServletContextListener {
  @Override
  protected Injector getInjector() {
    Module environmentModule;
    if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
      environmentModule = Modules.combine(
          new AppEngineDataStoreModule(),
          new GitKitUserAuthenticatorModule());
    } else {
      try {
        @SuppressWarnings("unchecked") // Generic cast
        Class<? extends Module> devEnvironmentModuleClass =
            (Class<? extends Module>)
                Class.forName("org.chaston.oakfunds.servlet.DevEnvironmentModule");
        environmentModule = devEnvironmentModuleClass.newInstance();
      } catch (ClassNotFoundException e) {
        throw new NoClassDefFoundError(e.getMessage());
      } catch (InstantiationException | IllegalAccessException e) {
        throw new IllegalStateException("DevEnvironmentModule was inaccessible", e);
      }
    }
    return Guice.createInjector(
        environmentModule,
        new AccountCodeModule(),
        new AppEngineModule(),
        new BootstrapModule(),
        new LedgerModule(),
        new ModelModule(),
        new UserSecurityModule(),
        new RecordTypeRegistryModule(),
        new StorageModule(),
        new SystemModule(),
        new XsrfModule());
  }
}
