package org.chaston.oakfunds.account;

import com.google.inject.Inject;
import org.chaston.oakfunds.storage.RecordFactory;
import org.chaston.oakfunds.storage.RecordType;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
class AccountCodeManagerImpl implements AccountCodeManager {

  private static final RecordFactory<AccountCode> RECORD_FACTORY =
      new RecordFactory<AccountCode>() {
        @Override
        public AccountCode newInstance(int id) {
          return new AccountCode(id);
        }

        @Override
        public RecordType getRecordType() {
          return RecordType.ACCOUNT_CODE;
        }
      };

  private static final String ATTRIBUTE_TITLE = "title";

  private final Store store;

  @Inject
  AccountCodeManagerImpl(Store store) {
    this.store = store;
  }

  @Override
  public AccountCode createAccountCode(int accountCodeNumber, String title) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_TITLE, title);
    return store.createRecord(RECORD_FACTORY, accountCodeNumber, attributes);
  }

  @Override
  public AccountCode getAccountCode(int accountCodeNumber) throws StorageException {
    return store.getRecord(RECORD_FACTORY, accountCodeNumber);
  }
}
