package org.chaston.oakfunds.account;

import org.chaston.oakfunds.storage.StorageException;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface AccountCodeManager {
  AccountCode createAccountCode(int accountCodeNumber, String title) throws StorageException;
  AccountCode getAccountCode(int accountCodeNumber) throws StorageException;
}
