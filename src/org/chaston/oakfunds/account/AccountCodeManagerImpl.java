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
package org.chaston.oakfunds.account;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.chaston.oakfunds.security.ActionType;
import org.chaston.oakfunds.security.Permission;
import org.chaston.oakfunds.security.PermissionAssertion;
import org.chaston.oakfunds.storage.SearchTerm;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
class AccountCodeManagerImpl implements AccountCodeManager {

  static final Permission PERMISSION_ACCOUNT_CODE_CREATE =
      Permission.builder("account_code.create")
          .addRelatedAction(AccountCode.TYPE, ActionType.CREATE).build();
  static final Permission PERMISSION_ACCOUNT_CODE_READ =
      Permission.builder("account_code.read")
          .addRelatedAction(AccountCode.TYPE, ActionType.READ).build();

  private final Store store;

  @Inject
  AccountCodeManagerImpl(Store store) {
    this.store = store;
  }

  @Override
  @PermissionAssertion("account_code.create")
  public AccountCode createAccountCode(int accountCodeNumber, String title)
      throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(AccountCode.ATTRIBUTE_TITLE, title);
    return store.createRecord(AccountCode.TYPE, accountCodeNumber, attributes);
  }

  @Override
  @PermissionAssertion("account_code.read")
  public AccountCode getAccountCode(int accountCodeNumber) throws StorageException {
    return store.getRecord(AccountCode.TYPE, accountCodeNumber);
  }

  @Override
  @PermissionAssertion("account_code.read")
  public Iterable<AccountCode> getAccountCodes() throws StorageException {
    return store.findRecords(AccountCode.TYPE, ImmutableList.<SearchTerm>of());
  }
}
