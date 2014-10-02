package org.chaston.oakfunds.ledger;

import org.chaston.oakfunds.storage.Attribute;
import org.chaston.oakfunds.storage.InstantRecord;
import org.chaston.oakfunds.storage.RecordType;
import org.joda.time.Instant;

import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public class AccountTransaction extends InstantRecord {

  @Attribute(name = "amount")
  private BigDecimal amount;

  @Attribute(name = "comment")
  private String comment;

  @Attribute(name = "sister_transaction_id", propertyName = "sisterTransactionId")
  private int sisterTransactionId;

  AccountTransaction(int id, Instant instant) {
    super(RecordType.ACCOUNT_TRANSACTION, id, instant);
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public int getSisterTransactionId() {
    return sisterTransactionId;
  }

  public void setSisterTransactionId(int sisterTransactionId) {
    this.sisterTransactionId = sisterTransactionId;
  }
}
