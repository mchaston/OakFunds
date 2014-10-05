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
package org.chaston.oakfunds.system;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.chaston.oakfunds.storage.FinalRecordFactory;
import org.chaston.oakfunds.storage.SearchTerm;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.Transaction;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
class SystemPropertiesManagerImpl implements SystemPropertiesManager {

  static final String PROPERTY_CURRENT_YEAR = "current_year";
  static final String PROPERTY_TIME_HORIZON = "time_horizon";

  static final String ATTRIBUTE_NAME = "name";
  static final String ATTRIBUTE_INTEGER_VALUE = "integer_value";

  private final Store store;
  private SystemProperty currentYear;
  private SystemProperty timeHorizon;

  @Inject
  SystemPropertiesManagerImpl(Store store,
      @Nullable Iterable<SystemPropertyLoader> bootstrappingSystemPropertyLoaders) throws StorageException {
    this.store = store;
    store.registerType(SystemProperty.TYPE,
        new FinalRecordFactory<SystemProperty>(SystemProperty.TYPE) {
          @Override
          protected SystemProperty newInstance(int id) {
            return new SystemProperty(id);
          }
        });

    if (bootstrappingSystemPropertyLoaders != null) {
      Transaction transaction = store.startTransaction();
      boolean successful = false;
      try {
        for (SystemPropertyLoader bootstrappingSystemPropertyLoader : bootstrappingSystemPropertyLoaders) {
          bootstrappingSystemPropertyLoader.load(store);
        }
        successful = true;
      } finally {
        if (successful) {
          transaction.commit();
        } else {
          transaction.rollback();
        }
      }
    }

    Iterable<SystemProperty> properties =
        store.findRecords(SystemProperty.TYPE, ImmutableList.<SearchTerm>of());
    for (SystemProperty property : properties) {
      if (PROPERTY_CURRENT_YEAR.equals(property.getName())) {
        currentYear = property;
      }
      if (PROPERTY_TIME_HORIZON.equals(property.getName())) {
        timeHorizon = property;
      }
    }
    if (currentYear == null) {
      throw new StorageException("The " + PROPERTY_CURRENT_YEAR + " property was not loaded.");
    }
    if (timeHorizon == null) {
      throw new StorageException("The " + PROPERTY_TIME_HORIZON + " property was not loaded.");
    }
  }

  @Override
  public int getCurrentYear() {
    return currentYear.getIntegerValue();
  }

  @Override
  public void setCurrentYear(int year) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_INTEGER_VALUE, year);
    currentYear = store.updateRecord(currentYear, attributes);
  }

  @Override
  public int getTimeHorizon() {
    return timeHorizon.getIntegerValue();
  }

  @Override
  public void setTimeHorizon(int years) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_INTEGER_VALUE, years);
    currentYear = store.updateRecord(currentYear, attributes);
  }
}
