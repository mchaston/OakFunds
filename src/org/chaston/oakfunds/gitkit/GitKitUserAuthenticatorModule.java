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
package org.chaston.oakfunds.gitkit;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.google.inject.servlet.ServletScopes;
import org.chaston.oakfunds.security.UserAuthenticator;

/**
 * TODO(mchaston): write JavaDocs
 */
public class GitKitUserAuthenticatorModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(UserAuthenticator.class).to(GitKitUserAuthenticator.class);
    bind(GitKitUserAuthenticator.class).in(Singleton.class);
    bind(AuthenticationState.class).in(ServletScopes.SESSION);
    install(new GitKitServletModule());
  }

  private class GitKitServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
      serve("/signout").with(SignoutServlet.class);
      bind(SignoutServlet.class).in(Singleton.class);

      serve("/gitkit_email").with(EmailRequestHandlerServlet.class);
      bind(EmailRequestHandlerServlet.class).in(Singleton.class);

      filter("/*").through(AuthenticationFilter.class);
      bind(AuthenticationFilter.class).in(Singleton.class);
    }
  }
}
