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
package org.chaston.oakfunds.ledger;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.chaston.oakfunds.storage.RecordType;
import org.chaston.oakfunds.storage.Store;

/**
 * TODO(mchaston): write JavaDocs
 */
public class LedgerModule extends AbstractModule {
  @Override
  protected void configure() {
    requireBinding(Store.class);
    bind(LedgerManagerImpl.class).in(Singleton.class);
    bind(LedgerManager.class).to(LedgerManagerImpl.class);

    Multibinder<RecordType> recordTypeMultibinder
        = Multibinder.newSetBinder(binder(), RecordType.class);
    recordTypeMultibinder.addBinding().toInstance(Account.TYPE);
    recordTypeMultibinder.addBinding().toInstance(AccountTransaction.TYPE);
    recordTypeMultibinder.addBinding().toInstance(BankAccount.TYPE);
    recordTypeMultibinder.addBinding().toInstance(BankAccountInterest.TYPE);
    recordTypeMultibinder.addBinding().toInstance(ExpenseAccount.TYPE);
    recordTypeMultibinder.addBinding().toInstance(RevenueAccount.TYPE);
  }
}
