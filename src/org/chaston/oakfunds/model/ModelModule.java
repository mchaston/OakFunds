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

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.chaston.oakfunds.storage.Store;
import org.chaston.oakfunds.system.SystemPropertiesManager;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ModelModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new ModelTypesModule());
    requireBinding(SystemPropertiesManager.class);
    requireBinding(Store.class);
    bind(ModelManagerImpl.class).in(Singleton.class);
    bind(ModelManager.class).to(ModelManagerImpl.class);
  }
}
