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
package org.chaston.oakfunds.security;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * TODO(mchaston): write JavaDocs
 */
public class SecurityModule extends AbstractModule {

  @Override
  protected void configure() {
    requireBinding(UserManager.class);
    bind(AuthorizationContext.class).to(AuthorizationContextImpl.class);
    bind(AuthenticationManager.class).to(AuthenticationManagerImpl.class);
    bind(AuthenticationManagerImpl.class).in(Scopes.SINGLETON);

    bind(AccessCounterMap.class).in(Scopes.SINGLETON);
    bind(RoleRegistry.class).in(Scopes.SINGLETON);

    bind(PermissionRegistry.class).in(Scopes.SINGLETON);

    bindInterceptor(Matchers.any(), Matchers.annotatedWith(PermissionAssertion.class),
        new PermissionAssertionInterceptor(getProvider(AuthorizationContext.class)));

    // Bind an empty set to ensure that there is always a value.
    Multibinder.newSetBinder(binder(), Permission.class);
  }

  private static class PermissionAssertionInterceptor implements MethodInterceptor {

    private final Provider<AuthorizationContext> authorizationContextProvider;

    private PermissionAssertionInterceptor(
        Provider<AuthorizationContext> authorizationContextProvider) {
      this.authorizationContextProvider = authorizationContextProvider;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
      PermissionAssertion permissionAssertion =
          methodInvocation.getMethod().getAnnotation(PermissionAssertion.class);
      try (SinglePermissionAssertion singlePermissionAssertion =
               authorizationContextProvider.get().assertPermission(permissionAssertion.value())) {
        return methodInvocation.proceed();
      }
    }
  }
}
