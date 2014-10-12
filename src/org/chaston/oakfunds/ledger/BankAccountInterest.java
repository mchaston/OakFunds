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

import org.chaston.oakfunds.storage.AttributeMethod;
import org.chaston.oakfunds.storage.IntervalRecord;
import org.chaston.oakfunds.storage.RecordTemporalType;
import org.chaston.oakfunds.storage.RecordType;

import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface BankAccountInterest extends IntervalRecord<BankAccountInterest> {

  public static final RecordType<BankAccountInterest> TYPE =
      new RecordType<>("bank_account_interest", BankAccountInterest.class,
          BankAccount.TYPE, RecordTemporalType.INTERVAL, true);

  String ATTRIBUTE_INTEREST_RATE = "interest_rate";

  @AttributeMethod(attribute = ATTRIBUTE_INTEREST_RATE, required = true)
  BigDecimal getInterestRate();
}
