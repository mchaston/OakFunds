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
import org.chaston.oakfunds.storage.InstantRecord;
import org.chaston.oakfunds.storage.ParentIdMethod;
import org.chaston.oakfunds.storage.RecordTemporalType;
import org.chaston.oakfunds.storage.RecordType;

import java.math.BigDecimal;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface AccountTransaction extends InstantRecord<AccountTransaction> {

  public static final RecordType<AccountTransaction> TYPE =
      new RecordType<>("account_transaction", AccountTransaction.class,
          RecordTemporalType.INSTANT, true);

  @AttributeMethod(attribute = "amount", required = true)
  BigDecimal getAmount();

  @AttributeMethod(attribute = "comment")
  String getComment();

  @AttributeMethod(attribute = "sister_transaction_id")
  int getSisterTransactionId();

  @ParentIdMethod
  int getAccountId();
}
