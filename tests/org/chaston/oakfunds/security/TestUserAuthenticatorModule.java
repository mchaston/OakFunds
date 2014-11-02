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

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.chaston.oakfunds.security.UserBootstrapModule.UserDef;
import org.chaston.oakfunds.storage.mgmt.SchemaDeploymentTask;

/**
 * TODO(mchaston): write JavaDocs
 */
public class TestUserAuthenticatorModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(UserAuthenticator.class).to(TestUserAuthenticator.class);

    install(new UserBootstrapModule() {
      @Override
      protected Class<? extends Provider<Iterable<UserDef>>> getUserDefsProviderClass() {
        return TestUserDefsProvider.class;
      }
    });
  }

  private static class TestUserDefsProvider implements Provider<Iterable<UserDef>> {
    @Inject
    TestUserDefsProvider(
        // Here for dependency enforcement.
        SchemaDeploymentTask schemaDeploymentTask) {
      // Do nothing.
    }

    @Override
    public Iterable<UserDef> get() {
      UserDef testUserDef = new UserDef(TestUserAuthenticator.TEST_USER_IDENTIFIER,
          "test@test.org", "Test User");
      testUserDef.addRoleGrant("admin");
      return ImmutableList.of(testUserDef);
    }
  }
}
