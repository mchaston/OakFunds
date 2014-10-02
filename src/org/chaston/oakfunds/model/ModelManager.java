package org.chaston.oakfunds.model;

import org.chaston.oakfunds.storage.StorageException;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface ModelManager {
  Model createNewModel(String title) throws StorageException;
  Model getModel(int modelId) throws StorageException;
}
