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
import com.google.inject.servlet.ServletModule;
import org.chaston.oakfunds.storage.Store;

/**
 * TODO(mchaston): write JavaDocs
 */
public class LedgerModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new LedgerTypesModule());
    requireBinding(Store.class);
    bind(LedgerManagerImpl.class).in(Singleton.class);
    bind(LedgerManager.class).to(LedgerManagerImpl.class);

    install(new LedgerServletModule());
  }

  private class LedgerServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
      serve("/ledger/accounts").with(AccountListServlet.class);
      bind(AccountListServlet.class).in(Singleton.class);
      serve("/ledger/bank_accounts").with(BankAccountListServlet.class);
      bind(BankAccountListServlet.class).in(Singleton.class);

      serve("/ledger/bank_account/create").with(BankAccountCreateServlet.class);
      bind(BankAccountCreateServlet.class).in(Singleton.class);
      serveRegex(BankAccountUpdateServlet.URI_REGEX).with(BankAccountUpdateServlet.class);
      bind(BankAccountUpdateServlet.class).in(Singleton.class);

      serve("/ledger/expense_account/create").with(ExpenseAccountCreateServlet.class);
      bind(ExpenseAccountCreateServlet.class).in(Singleton.class);
      serveRegex(ExpenseAccountUpdateServlet.URI_REGEX).with(ExpenseAccountUpdateServlet.class);
      bind(ExpenseAccountUpdateServlet.class).in(Singleton.class);

      serve("/ledger/revenue_account/create").with(RevenueAccountCreateServlet.class);
      bind(RevenueAccountCreateServlet.class).in(Singleton.class);
      serveRegex(RevenueAccountUpdateServlet.URI_REGEX).with(RevenueAccountUpdateServlet.class);
      bind(RevenueAccountUpdateServlet.class).in(Singleton.class);
    }
  }
}
