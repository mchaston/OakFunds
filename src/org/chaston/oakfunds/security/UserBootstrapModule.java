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
package org.chaston.oakfunds.security;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.chaston.oakfunds.bootstrap.BootstrapConfigLoader;
import org.chaston.oakfunds.bootstrap.BootstrapTask;
import org.chaston.oakfunds.bootstrap.TransactionalBootstrapTask;
import org.chaston.oakfunds.storage.Store;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO(mchaston): write JavaDocs
 */
public class UserBootstrapModule extends AbstractModule {

  public static final int BOOTSTRAP_TASK_PRIORITY = 20;

  @Override
  protected void configure() {
    requireBinding(Store.class);

    install(new SystemSecurityModule());

    Multibinder<BootstrapConfigLoader> bootstrapConfigLoaderBinder =
        Multibinder.newSetBinder(binder(), BootstrapConfigLoader.class);
    bootstrapConfigLoaderBinder.addBinding().to(UserBootstrapConfigLoader.class);
    bind(UserBootstrapConfigLoader.class).in(Singleton.class);

    bind(new TypeLiteral<Iterable<UserDef>>() {})
        .toProvider(getUserDefsProviderClass());

    Multibinder<BootstrapTask> bootstrapTaskBinder =
        Multibinder.newSetBinder(binder(), BootstrapTask.class);
    bootstrapTaskBinder.addBinding().to(UserBootstrapTask.class);
  }

  protected Class<? extends Provider<Iterable<UserDef>>> getUserDefsProviderClass() {
    return UserBootstrapConfigLoader.class;
  }

  private static class UserBootstrapTask extends TransactionalBootstrapTask {
    private final UserManager userManager;
    private final Provider<Iterable<UserDef>> userDefsProvider;

    @Inject
    UserBootstrapTask(
        Store store,
        Provider<Iterable<UserDef>> userDefsProvider,
        UserManager userManager) {
      super(store);
      this.userDefsProvider = userDefsProvider;
      this.userManager = userManager;
    }

    @Override
    public String getName() {
      return "users";
    }

    @Override
    protected void bootstrapDuringTransaction() throws Exception {
      for (UserDef userDef : userDefsProvider.get()) {
        User user = userManager.getUser(userDef.identifier);
        if (user == null) {
          user = userManager.createUser(userDef.identifier, userDef.email, userDef.name);
        }
        Set<String> missingRoleGrants = new HashSet<>(userDef.getRoleGrants());
        Iterable<RoleGrant> existingRoleGrants = userManager.getRoleGrants(user);
        for (RoleGrant existingRoleGrant : existingRoleGrants) {
          missingRoleGrants.remove(existingRoleGrant.getName());
        }
        for (String roleGrant : missingRoleGrants) {
          userManager.grantRole(user, roleGrant);
        }
      }
    }

    @Override
    public int getPriority() {
      return BOOTSTRAP_TASK_PRIORITY;
    }
  }

  private static class UserBootstrapConfigLoader
      implements BootstrapConfigLoader, Provider<Iterable<UserDef>> {
    private final List<UserDef> userDefs = new ArrayList<>();

    @Override
    public String getConfigElementName() {
      return "user_config";
    }

    @Override
    public DefaultHandler getDefaultHandler() {
      return new UserConfigHandler();
    }

    @Override
    public Iterable<UserDef> get() {
      return userDefs;
    }

    private class UserConfigHandler extends DefaultHandler {
      private UserDef userDef;

      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes)
          throws SAXException {
        if (qName.equals("user")) {
          if (userDef != null) {
            throw new SAXException("Unexpected user element while previous user is being handed.");
          }
          userDef = new UserDef(attributes.getValue("identifier"),
              attributes.getValue("email"), attributes.getValue("name"));
          userDefs.add(userDef);
        } else if (qName.equals("role_grant")) {
          if (userDef == null) {
            throw new SAXException("Unexpected role_grant element no user is being handed.");
          }
          userDef.addRoleGrant(attributes.getValue("name"));
        }
      }

      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("user")) {
          userDef = null;
        }
      }
    }
  }

  static class UserDef {
    private final String identifier;
    private final String email;
    private final String name;
    private final Set<String> roleGrants = new HashSet<>();

    UserDef(String identifier, String email, String name) {
      this.identifier = identifier;
      this.email = email;
      this.name = name;
    }

    void addRoleGrant(String roleName) {
      roleGrants.add(roleName);
    }

    Set<String> getRoleGrants() {
      return roleGrants;
    }
  }
}
