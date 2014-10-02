package org.chaston.oakfunds.ledger;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.chaston.oakfunds.storage.Store;

/**
 * TODO(mchaston): write JavaDocs
 */
public class LedgerModule extends AbstractModule {
  @Override
  protected void configure() {
    requireBinding(Store.class);
    bind(LedgerManagerImpl.class).in(Singleton.class);
    bind(LedgerManager.class).to(LedgerManagerImpl.class);
  }
}
