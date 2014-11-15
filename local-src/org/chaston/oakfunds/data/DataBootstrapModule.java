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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import org.chaston.oakfunds.account.AccountCode;
import org.chaston.oakfunds.account.AccountCodeManager;
import org.chaston.oakfunds.bootstrap.BootstrapTask;
import org.chaston.oakfunds.bootstrap.TransactionalBootstrapTask;
import org.chaston.oakfunds.ledger.BankAccount;
import org.chaston.oakfunds.ledger.BankAccountType;
import org.chaston.oakfunds.ledger.ExpenseAccount;
import org.chaston.oakfunds.ledger.LedgerManager;
import org.chaston.oakfunds.ledger.RevenueAccount;
import org.chaston.oakfunds.storage.Record;
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
    requireBinding(LedgerManager.class);
    requireBinding(Store.class);

    Multibinder<BootstrapTask> bootstrapTaskBinder =
        Multibinder.newSetBinder(binder(), BootstrapTask.class);
    bootstrapTaskBinder.addBinding().to(DataBootstrapTask.class);
  }

  private static class DataBootstrapTask extends TransactionalBootstrapTask {
    private final AccountCodeManager accountCodeManager;
    private final LedgerManager ledgerManager;

    @Inject
    DataBootstrapTask(
        Store store,
        AccountCodeManager accountCodeManager,
        LedgerManager ledgerManager) {
      super(store);
      this.accountCodeManager = accountCodeManager;
      this.ledgerManager = ledgerManager;
    }

    @Override
    public String getName() {
      return "data";
    }

    @Override
    protected void bootstrapDuringTransaction() throws Exception {
      ImmutableMap<Integer, AccountCode> accountCodes = bootstrapAccountCodes();
      ImmutableMap<Integer, BankAccount> bankAccounts = bootstrapBankAccounts(accountCodes);
      ImmutableMap<Integer, RevenueAccount> revenueAccounts =
          bootstrapRevenueAccounts(accountCodes, bankAccounts);
      ImmutableMap<Integer, ExpenseAccount> expenseAccounts =
          bootstrapExpenseAccounts(accountCodes, bankAccounts);
    }

    private ImmutableMap<Integer, AccountCode> bootstrapAccountCodes() throws StorageException{
      RecordMapBuilder<AccountCode> accountCodes = new RecordMapBuilder<>();
      accountCodes.add(accountCodeManager.createAccountCode(100, "First account code"));
      accountCodes.add(accountCodeManager.createAccountCode(200, "Second account code"));
      accountCodes.add(accountCodeManager.createAccountCode(300, "Third account code"));
      return accountCodes.build();
    }

    private ImmutableMap<Integer, BankAccount> bootstrapBankAccounts(
        ImmutableMap<Integer, AccountCode> accountCodes)
        throws StorageException {
      RecordMapBuilder<BankAccount> bankAccounts = new RecordMapBuilder<>();
      bankAccounts.add(ledgerManager.createBankAccount(accountCodes.get(100),
          "Current Account", BankAccountType.OPERATING));
      bankAccounts.add(ledgerManager.createBankAccount(accountCodes.get(100),
          "Reserve", BankAccountType.RESERVE));
      bankAccounts.add(ledgerManager.createBankAccount(accountCodes.get(100),
          "CD 1", BankAccountType.RESERVE));
      return bankAccounts.build();
    }

    private ImmutableMap<Integer, RevenueAccount> bootstrapRevenueAccounts(
        ImmutableMap<Integer, AccountCode> accountCodes,
        ImmutableMap<Integer, BankAccount> bankAccounts)
        throws StorageException {
      RecordMapBuilder<RevenueAccount> revenueAccounts = new RecordMapBuilder<>();
      revenueAccounts.add(ledgerManager.createRevenueAccount(accountCodes.get(200),
          "HOA Dues",
          Iterables.find(bankAccounts.values(),
              new Predicate<BankAccount>() {
                @Override
                public boolean apply(BankAccount bankAccount) {
                  return bankAccount.getTitle().equals("Current Account");
                }
              })));
      return revenueAccounts.build();
    }

    private ImmutableMap<Integer, ExpenseAccount> bootstrapExpenseAccounts(
        ImmutableMap<Integer, AccountCode> accountCodes,
        ImmutableMap<Integer, BankAccount> bankAccounts)
        throws StorageException {
      RecordMapBuilder<ExpenseAccount> expenseAccounts = new RecordMapBuilder<>();
      expenseAccounts.add(ledgerManager.createExpenseAccount(accountCodes.get(300),
          "Trash and Recycling",
          Iterables.find(bankAccounts.values(),
              new Predicate<BankAccount>() {
                @Override
                public boolean apply(BankAccount bankAccount) {
                  return bankAccount.getTitle().equals("Current Account");
                }
              })));
      return expenseAccounts.build();
    }

    @Override
    public int getPriority() {
      return BOOTSTRAP_TASK_PRIORITY;
    }
  }

  private static class RecordMapBuilder<T extends Record<T>> {
    private final ImmutableMap.Builder<Integer, T> records = ImmutableMap.builder();

    void add(T record) {
      records.put(record.getId(), record);
    }

    ImmutableMap<Integer, T> build() {
      return records.build();
    }
  }
}
