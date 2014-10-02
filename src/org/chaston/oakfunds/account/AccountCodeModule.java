package org.chaston.oakfunds.account;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.chaston.oakfunds.storage.Store;

/**
 * TODO(mchaston): write JavaDocs
 */
public class AccountCodeModule extends AbstractModule {
  @Override
  protected void configure() {
    requireBinding(Store.class);
    bind(AccountCodeManagerImpl.class).in(Singleton.class);
    bind(AccountCodeManager.class).to(AccountCodeManagerImpl.class);
  }
}
