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
package org.chaston.oakfunds.appengine;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import org.chaston.oakfunds.security.SystemAuthenticationManager;
import org.chaston.oakfunds.security.UserAuthenticator;
import org.chaston.oakfunds.security.UserManager;
import org.chaston.oakfunds.storage.Store;

/**
 * TODO(mchaston): write JavaDocs
 */
public class AppEngineModule extends AbstractModule {
  @Override
  protected void configure() {
    requireBinding(Store.class);
    requireBinding(UserAuthenticator.class);
    requireBinding(SystemAuthenticationManager.class);
    requireBinding(UserManager.class);

    install(new AppEngineServletModule());
  }

  @Provides
  UserService provideUserService() {
    return UserServiceFactory.getUserService();
  }

  private class AppEngineServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
      serve("/bootstrap_admin").with(AdminBootstrapServlet.class);
      bind(AdminBootstrapServlet.class).in(Singleton.class);
    }
  }
}
