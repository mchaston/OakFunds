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
package org.chaston.oakfunds.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO(mchaston): write JavaDocs
 */
class TransactionImpl implements Transaction {

  private static final Logger logger = Logger.getLogger(TransactionImpl.class.getName());

  private final StoreImpl store;
  private final Connection connection;

  TransactionImpl(StoreImpl store, Connection connection) throws SQLException {
    this.store = store;
    this.connection = connection;
    connection.setAutoCommit(false);
  }

  Connection getConnection() {
    return connection;
  }

  @Override
  public void commit() throws StorageException {
    try {
      connection.commit();
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to commit transaction", e);
      throw new StorageException("Failed to commit transaction", e);
    } finally {
      store.endTransaction(connection);
    }
  }

  @Override
  public void rollback() {
    try {
      connection.rollback();
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Failed to roll back transaction", e);
    } finally {
      store.endTransaction(connection);
    }
  }
}
