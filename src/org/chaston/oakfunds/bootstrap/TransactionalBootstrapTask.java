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
package org.chaston.oakfunds.bootstrap;

import com.google.common.base.Preconditions;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.Transaction;

/**
 * TODO(mchaston): write JavaDocs
 */
public abstract class TransactionalBootstrapTask implements BootstrapTask {

  private final Store store;

  protected TransactionalBootstrapTask(Store store) {
    this.store = Preconditions.checkNotNull(store, "store");
  }

  protected Store getStore() {
    return store;
  }

  @Override
  public final void bootstrap() throws Exception {
    Transaction transaction = store.startTransaction();
    boolean successful = false;
    try {
      bootstrapDuringTransaction();
      successful = true;
    } finally {
      if (successful) {
        transaction.commit();
      } else {
        transaction.rollback();
      }
    }
  }

  protected abstract void bootstrapDuringTransaction() throws Exception;
}
