package org.chaston.oakfunds.ledger;

import org.chaston.oakfunds.account.AccountCode;
import org.chaston.oakfunds.storage.StorageException;
import org.joda.time.Instant;

import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface LedgerManager {
  BankAccount getBankAccount(int id) throws StorageException;
  BankAccount createBankAccount(AccountCode accountCode, String title) throws StorageException;
  void setInterestRate(BankAccount bankAccount, BigDecimal interestRate, Instant start, Instant end) throws StorageException;
  BigDecimal getInterestRate(BankAccount bankAccount, Instant date) throws StorageException;

  BigDecimal getBalance(BankAccount bankAccount, Instant date) throws StorageException;

  ExpenseAccount createExpenseAccount(AccountCode accountCode, String title, BankAccount defaultSourceAccount) throws StorageException;

  RevenueAccount createRevenueAccount(AccountCode accountCode, String title, BankAccount defaultDepositAccount) throws StorageException;

  void recordTransaction(Account account, Instant date, BigDecimal amount) throws StorageException;
  void recordTransaction(Account account, Instant date, BigDecimal amount, String comment) throws StorageException;

}
