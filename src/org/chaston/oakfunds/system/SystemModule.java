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

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import org.chaston.oakfunds.bootstrap.BootstrappingDependency;
import org.chaston.oakfunds.security.AuthorizationContext;
import org.chaston.oakfunds.security.SystemAuthenticationManager;
import org.chaston.oakfunds.storage.Store;

/**
 * TODO(mchaston): write JavaDocs
 */
public class SystemModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new SystemTypesModule());
    requireBinding(Store.class);
    requireBinding(BootstrappingDependency.class);
    requireBinding(AuthorizationContext.class);
    requireBinding(SystemAuthenticationManager.class);
    bind(SystemPropertiesManagerImpl.class).in(Singleton.class);
    bind(SystemPropertiesManager.class).to(SystemPropertiesManagerImpl.class);
    install(new SystemServletModule());
  }

  private class SystemServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
      serve("/system/system_properties").with(SystemPropertyListServlet.class);
      bind(SystemPropertyListServlet.class).in(Singleton.class);
    }
  }
}
