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

import org.chaston.oakfunds.storage.StorageException;

/**
 * TODO(mchaston): write JavaDocs
 */
public interface SystemPropertiesManager {
  /**
   * Gets the current year (since epoch) where models transition to live accounts.
   */
  int getCurrentYear();

  /**
   * Sets the current year (since epoch) where models transition to live accounts.
   *
   * <p>This may be an expensive operation as it will make the models update.
   */
  void setCurrentYear(int year) throws StorageException;

  /**
   * Gets the number of years after {@link #getCurrentYear()} to populate.
   */
  int getTimeHorizon();

  /**
   * Sets the number of years after {@link #getCurrentYear()} to populate.
   *
   * <p>This may be an expensive operation as it will make the models update.
   */
  void setTimeHorizon(int years) throws StorageException;

  Iterable<SystemProperty> getSystemProperties() throws StorageException;
}
