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
