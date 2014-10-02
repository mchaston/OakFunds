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
package org.chaston.oakfunds.model;

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
class ModelManagerImpl implements ModelManager {
  private static final RecordFactory<Model> MODEL_RECORD_FACTORY = new RecordFactory<Model>() {
    @Override
    public Model newInstance(int id) {
      return new Model(id);
    }

    @Override
    public RecordType getRecordType() {
      return RecordType.MODEL;
    }
  };

  private static final String ATTRIBUTE_TITLE = "title";

  private final Store store;

  @Inject
  ModelManagerImpl(Store store) {
    this.store = store;
  }

  @Override
  public Model createNewModel(String title) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_TITLE, title);
    return store.createRecord(MODEL_RECORD_FACTORY, attributes);
  }

  @Override
  public Model getModel(int modelId) throws StorageException {
    return store.getRecord(MODEL_RECORD_FACTORY, modelId);
  }
}
