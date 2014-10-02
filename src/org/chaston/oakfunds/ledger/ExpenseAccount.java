package org.chaston.oakfunds.ledger;

import org.chaston.oakfunds.storage.Attribute;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ExpenseAccount extends Account {

  @Attribute(name = "default_source_account_id", propertyName = "defaultSourceAccountId")
  private int defaultSourceAccountId;

  ExpenseAccount(int id) {
    super(RecordType.EXPENSE_ACCOUNT, id);
  }

  public int getDefaultSourceAccountId() {
    return defaultSourceAccountId;
  }

  public void setDefaultSourceAccountId(int defaultSourceAccountId) {
    this.defaultSourceAccountId = defaultSourceAccountId;
  }
}
