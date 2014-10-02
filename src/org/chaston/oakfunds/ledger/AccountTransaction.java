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
