package org.chaston.oakfunds.storage;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface Transaction {
  void commit() throws StorageException;
  void rollback();
}
