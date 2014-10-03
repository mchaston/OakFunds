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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.chaston.oakfunds.account.AccountCodeManager;
import org.chaston.oakfunds.account.AccountCodeModule;
import org.chaston.oakfunds.storage.StorageException;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.storage.TestStorageModule;
import org.chaston.oakfunds.storage.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class ModelManagerTest {

  @Inject
  private AccountCodeManager accountCodeManager;
  @Inject
  private ModelManager modelManager;
  @Inject
  private Store store;

  @Before
  public void setUp() {
    Injector injector = Guice.createInjector(
        new AccountCodeModule(),
        new ModelModule(),
        new TestStorageModule());
    injector.injectMembers(this);
  }

  @Test
  public void createModel() throws StorageException {
    Transaction transaction = store.startTransaction();
    Model model = modelManager.createNewModel("New Model");
    assertEquals("New Model", model.getTitle());
    transaction.commit();

    assertEquals("New Model", modelManager.getModel(model.getId()).getTitle());
  }

  @Test
  public void getBaseModel() throws StorageException {
    assertNotNull(modelManager.getBaseModel());
  }
}
