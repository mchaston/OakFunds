package org.chaston.oakfunds.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * TODO(mchaston): write JavaDocs
 */
public class TestStorageModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(InMemoryStore.class).in(Singleton.class);
    bind(Store.class).to(InMemoryStore.class);
  }
}
