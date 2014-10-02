package org.chaston.oakfunds.ledger;

import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public class BankAccount extends Account {
  BankAccount(int id) {
    super(RecordType.BANK_ACCOUNT, id);
  }
}
