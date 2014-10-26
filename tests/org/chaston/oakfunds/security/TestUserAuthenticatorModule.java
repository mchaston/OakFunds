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
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import org.chaston.oakfunds.bootstrap.BootstrapTask;
import org.chaston.oakfunds.bootstrap.TransactionalBootstrapTask;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.mgmt.SchemaDeploymentTask;

/**
 * TODO(mchaston): write JavaDocs
 */
public class TestUserAuthenticatorModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(UserAuthenticator.class).to(TestUserAuthenticator.class);

    Multibinder<BootstrapTask> bootstrapTaskBinder =
        Multibinder.newSetBinder(binder(), BootstrapTask.class);
    bootstrapTaskBinder.addBinding().to(TestUserBootstrapTask.class);
  }

  private static class TestUserBootstrapTask extends TransactionalBootstrapTask {
    private final UserManager userManager;

    @Inject
    TestUserBootstrapTask(
        SchemaDeploymentTask schemaDeploymentTask, // Here for dependency enforcement.
        Store store,
        UserManager userManager) {
      super(store);
      this.userManager = userManager;
    }

    @Override
    protected void bootstrapDuringTransaction() throws Exception {
      User user = userManager.createUser(TestUserAuthenticator.TEST_USER_IDENTIFIER, "Test User");
      userManager.grantRole(user, "admin");
    }
  }
}
