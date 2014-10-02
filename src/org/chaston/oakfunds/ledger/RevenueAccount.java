package org.chaston.oakfunds.ledger;

import org.chaston.oakfunds.storage.Attribute;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public class RevenueAccount extends Account {
  @Attribute(name = "default_deposit_account_id", propertyName = "defaultDepositAccountId")
  private int defaultDepositAccountId;

  RevenueAccount(int id) {
    super(RecordType.REVENUE_ACCOUNT, id);
  }

  public int getDefaultDepositAccountId() {
    return defaultDepositAccountId;
  }

  public void setDefaultDepositAccountId(int defaultDepositAccountId) {
    this.defaultDepositAccountId = defaultDepositAccountId;
  }
}
