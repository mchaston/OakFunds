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
import org.chaston.oakfunds.bootstrap.BootstrappingDependency;
import org.chaston.oakfunds.security.ActionType;
import org.chaston.oakfunds.security.AuthenticationScope;
import org.chaston.oakfunds.security.AuthorizationContext;
import org.chaston.oakfunds.security.Permission;
import org.chaston.oakfunds.security.PermissionAssertion;
import org.chaston.oakfunds.security.SinglePermissionAssertion;
import org.chaston.oakfunds.security.SystemAuthenticationManager;
import org.chaston.oakfunds.storage.OrderingTerm;
import org.chaston.oakfunds.storage.SearchTerm;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
class SystemPropertiesManagerImpl implements SystemPropertiesManager {

  static final String PROPERTY_CURRENT_YEAR = "current_year";
  static final String PROPERTY_TIME_HORIZON = "time_horizon";

  static final Permission PERMISSION_SYSTEM_PROPERTY_READ =
      Permission.builder("system_property.read")
          .addRelatedAction(SystemProperty.TYPE, ActionType.READ).build();
  static final Permission PERMISSION_SYSTEM_PROPERTY_CREATE =
      Permission.builder("system_property.create")
          .addRelatedAction(SystemProperty.TYPE, ActionType.CREATE).build();

  static final Permission PERMISSION_CURRENT_YEAR_UPDATE =
      Permission.builder("current_year.update")
          .addRelatedAction(SystemProperty.TYPE, ActionType.UPDATE).build();
  static final Permission PERMISSION_TIME_HORIZON_UPDATE =
      Permission.builder("time_horizon.update")
          .addRelatedAction(SystemProperty.TYPE, ActionType.UPDATE).build();

  private final Store store;
  private SystemProperty currentYear;
  private SystemProperty timeHorizon;

  @Inject
  SystemPropertiesManagerImpl(
      BootstrappingDependency bootstrappingDependency, // Here for dependency enforcement.
      Store store,
      AuthorizationContext authorizationContext,
      SystemAuthenticationManager authenticationManager) throws StorageException {
    this.store = store;

    try (AuthenticationScope authenticationScope = authenticationManager.authenticateSystem()) {
      try (SinglePermissionAssertion singlePermissionAssertion =
               authorizationContext.assertPermission("system_property.read")) {
        Iterable<SystemProperty> properties =
            store.findRecords(SystemProperty.TYPE, ImmutableList.<SearchTerm>of(),
                ImmutableList.<OrderingTerm>of());
        for (SystemProperty property : properties) {
          if (PROPERTY_CURRENT_YEAR.equals(property.getName())) {
            currentYear = property;
          }
          if (PROPERTY_TIME_HORIZON.equals(property.getName())) {
            timeHorizon = property;
          }
        }
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
  @PermissionAssertion("system_property.read")
  public Iterable<SystemProperty> getSystemProperties() throws StorageException {
    return store.findRecords(SystemProperty.TYPE, ImmutableList.<SearchTerm>of(),
        ImmutableList.<OrderingTerm>of());
  }

  @Override
  public int getCurrentYear() {
    return currentYear.getIntegerValue();
  }

  @Override
  @PermissionAssertion("current_year.update")
  public void setCurrentYear(int year) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(SystemProperty.ATTRIBUTE_INTEGER_VALUE, year);
    currentYear = store.updateRecord(currentYear, attributes);
  }

  @Override
  public int getTimeHorizon() {
    return timeHorizon.getIntegerValue();
  }

  @Override
  @PermissionAssertion("time_horizon.update")
  public void setTimeHorizon(int years) throws StorageException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(SystemProperty.ATTRIBUTE_INTEGER_VALUE, years);
    currentYear = store.updateRecord(currentYear, attributes);
  }
}
