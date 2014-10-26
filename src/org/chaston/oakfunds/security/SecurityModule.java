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

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.chaston.oakfunds.storage.RecordType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    bind(RoleRegistry.class).in(Scopes.SINGLETON);

    bind(new TypeLiteral<Map<RecordType, Map<ActionType, AtomicInteger>>>() {})
        .toProvider(AccessCountersProvider.class);
    bind(new TypeLiteral<ThreadLocal<Map<RecordType, Map<ActionType, AtomicInteger>>>>() {})
       .toInstance(new ThreadLocal<Map<RecordType, Map<ActionType, AtomicInteger>>>());

    bind(PermissionRegistry.class).in(Scopes.SINGLETON);
    bind(SinglePermissionAssertionFactory.class).to(SinglePermissionAssertionFactoryImpl.class);

    bindInterceptor(Matchers.any(), Matchers.annotatedWith(PermissionAssertion.class),
        new PermissionAssertionInterceptor(getProvider(AuthorizationContext.class)));

    // Bind an empty set to ensure that there is always a value.
    Multibinder.newSetBinder(binder(), Permission.class);
  }

  private static class AccessCountersProvider
      implements Provider<Map<RecordType, Map<ActionType, AtomicInteger>>> {

    private final ThreadLocal<Map<RecordType, Map<ActionType, AtomicInteger>>> accessCounters;

    @Inject
    AccessCountersProvider(
        ThreadLocal<Map<RecordType, Map<ActionType, AtomicInteger>>> accessCounters) {
      this.accessCounters = accessCounters;
    }

    @Override
    public Map<RecordType, Map<ActionType, AtomicInteger>> get() {
      Map<RecordType, Map<ActionType, AtomicInteger>> accessCounters = this.accessCounters.get();
      return accessCounters == null ? null : Collections.unmodifiableMap(accessCounters);
    }
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

  private static class SinglePermissionAssertionFactoryImpl
      implements SinglePermissionAssertionFactory {

    private final ThreadLocal<Map<RecordType, Map<ActionType, AtomicInteger>>> accessCountersThreadLocal;
    private final PermissionRegistry permissionRegistry;

    @Inject
    SinglePermissionAssertionFactoryImpl(
        ThreadLocal<Map<RecordType, Map<ActionType, AtomicInteger>>> accessCountersThreadLocal,
        PermissionRegistry permissionRegistry) {
      this.accessCountersThreadLocal = accessCountersThreadLocal;
      this.permissionRegistry = permissionRegistry;
    }

    @Override
    public SinglePermissionAssertion create(String permissionName) {
      Permission permission = permissionRegistry.getPermission(permissionName);
      if (permission == null) {
        throw new IllegalArgumentException("Permission " + permissionName + " does not exist.");
      }
      return new SinglePermissionAssertionImpl(accessCountersThreadLocal, permission);
    }
  }

  private static class SinglePermissionAssertionImpl implements SinglePermissionAssertion {

    private static final Supplier<Map<ActionType, AtomicInteger>> ACTION_COUNTER_MAP_SUPPLIER =
        new Supplier<Map<ActionType, AtomicInteger>>() {
          @Override
          public Map<ActionType, AtomicInteger> get() {
            return new HashMap<>();
          }
        };

    private static final Supplier<AtomicInteger> ATOMIC_INTEGER_SUPPLIER =
        new Supplier<AtomicInteger>() {
          @Override
          public AtomicInteger get() {
            return new AtomicInteger();
          }
        };

    private final ThreadLocal<Map<RecordType, Map<ActionType, AtomicInteger>>>
        accessCountersThreadLocal;
    private final boolean firstAssertion;
    private final List<AtomicInteger> actionTypeCounts;

    public SinglePermissionAssertionImpl(
        ThreadLocal<Map<RecordType, Map<ActionType, AtomicInteger>>> accessCountersThreadLocal,
        Permission permission) {
      this.accessCountersThreadLocal = accessCountersThreadLocal;
      Map<RecordType, Map<ActionType, AtomicInteger>> accessCounters =
          accessCountersThreadLocal.get();
      if (accessCounters == null) {
        firstAssertion = true;
        accessCounters = new HashMap<>();
        accessCountersThreadLocal.set(accessCounters);
      } else {
        firstAssertion = false;
      }

      ImmutableList.Builder<AtomicInteger> actionTypeCounts = ImmutableList.builder();
      for (Map.Entry<RecordType, ActionType> entry : permission.getRelatedActions().entries()) {
        Map<ActionType, AtomicInteger> actionCounters =
            get(accessCounters, entry.getKey(), ACTION_COUNTER_MAP_SUPPLIER);
        AtomicInteger actionTypeCount =
            get(actionCounters, entry.getValue(), ATOMIC_INTEGER_SUPPLIER);
        actionTypeCount.incrementAndGet();
        actionTypeCounts.add(actionTypeCount);
      }
      this.actionTypeCounts = actionTypeCounts.build();
    }

    private <K, V> V get(Map<K, V> map, K key, Supplier<V> valueSupplier) {
      V value = map.get(key);
      if (value == null) {
        value = valueSupplier.get();
        map.put(key, value);
      }
      return value;
    }

    @Override
    public void close() {
      for (AtomicInteger actionTypeCount : actionTypeCounts) {
        actionTypeCount.decrementAndGet();
      }
      if (firstAssertion) {
        accessCountersThreadLocal.remove();
      }
    }
  }
}
