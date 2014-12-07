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
import org.chaston.oakfunds.storage.RecordType;

import javax.annotation.Nullable;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface RevenueAccount extends Account<RevenueAccount> {

  static final RecordType<RevenueAccount> TYPE =
      RecordType.builder("revenue_account", RevenueAccount.class)
          .extensionOf(Account.TYPE)
          .build();

  String ATTRIBUTE_DEFAULT_DEPOSIT_ACCOUNT_ID = "default_deposit_account_id";

  @Nullable
  @AttributeMethod(attribute = ATTRIBUTE_DEFAULT_DEPOSIT_ACCOUNT_ID)
  Integer getDefaultDepositAccountId();
}
