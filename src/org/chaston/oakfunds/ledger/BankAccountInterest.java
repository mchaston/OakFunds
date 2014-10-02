package org.chaston.oakfunds.ledger;

import org.chaston.oakfunds.storage.Attribute;
import org.chaston.oakfunds.storage.IntervalRecord;
import org.chaston.oakfunds.storage.RecordType;
import org.joda.time.Instant;

import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public class BankAccountInterest extends IntervalRecord {

  @Attribute(name = "interest_rate", propertyName = "interestRate")
  private BigDecimal interestRate;

  protected BankAccountInterest(int id, Instant start, Instant end) {
    super(RecordType.BANK_ACCOUNT_INTEREST, id, start, end);
  }

  public BigDecimal getInterestRate() {
    return interestRate;
  }

  public void setInterestRate(BigDecimal interestRate) {
    this.interestRate = interestRate;
  }
}
