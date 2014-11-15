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
package org.chaston.oakfunds.data;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import org.chaston.oakfunds.account.AccountCodeManager;
import org.chaston.oakfunds.bootstrap.BootstrapTask;
import org.chaston.oakfunds.bootstrap.TransactionalBootstrapTask;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.system.SystemBootstrapModule;

/**
 * TODO(mchaston): write JavaDocs
 */
public class DataBootstrapModule extends AbstractModule {

  public static final int BOOTSTRAP_TASK_PRIORITY =
      SystemBootstrapModule.BOOTSTRAP_TASK_PRIORITY + 10;

  @Override
  protected void configure() {
    requireBinding(AccountCodeManager.class);
    requireBinding(Store.class);

    Multibinder<BootstrapTask> bootstrapTaskBinder =
        Multibinder.newSetBinder(binder(), BootstrapTask.class);
    bootstrapTaskBinder.addBinding().to(DataBootstrapTask.class);
  }

  private static class DataBootstrapTask extends TransactionalBootstrapTask {
    private final AccountCodeManager accountCodeManager;

    @Inject
    DataBootstrapTask(
        Store store,
        AccountCodeManager accountCodeManager) {
      super(store);
      this.accountCodeManager = accountCodeManager;
    }

    @Override
    public String getName() {
      return "data";
    }

    @Override
    protected void bootstrapDuringTransaction() throws Exception {
      bootstrapAccountCodes();
    }

    private void bootstrapAccountCodes() throws StorageException{
      accountCodeManager.createAccountCode(100, "First account code");
      accountCodeManager.createAccountCode(200, "Second account code");
      accountCodeManager.createAccountCode(300, "Third account code");
    }

    @Override
    public int getPriority() {
      return BOOTSTRAP_TASK_PRIORITY;
    }
  }
}
