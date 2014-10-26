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
import com.google.inject.Singleton;
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
  @Override
  protected void configure() {
    requireBinding(Store.class);

    install(new UserTypesModule());
    bind(UserManager.class).to(UserManagerImpl.class);
    install(new SystemSecurityModule());

    Multibinder<BootstrapConfigLoader> bootstrapConfigLoaderBinder =
        Multibinder.newSetBinder(binder(), BootstrapConfigLoader.class);
    bootstrapConfigLoaderBinder.addBinding().to(UserBootstrapConfigLoader.class);
    bind(UserBootstrapConfigLoader.class).in(Singleton.class);

    Multibinder<BootstrapTask> bootstrapTaskBinder =
        Multibinder.newSetBinder(binder(), BootstrapTask.class);
    bootstrapTaskBinder.addBinding().to(UserBootstrapTask.class);
  }

  private static class UserBootstrapTask extends TransactionalBootstrapTask {
    private final UserBootstrapConfigLoader userBootstrapConfigLoader;
    private final UserManager userManager;

    @Inject
    UserBootstrapTask(
        Store store,
        UserBootstrapConfigLoader userBootstrapConfigLoader,
        UserManager userManager) {
      super(store);
      this.userBootstrapConfigLoader = userBootstrapConfigLoader;
      this.userManager = userManager;
    }

    @Override
    protected void bootstrapDuringTransaction() throws Exception {
      for (UserDef userDef : userBootstrapConfigLoader.getUserDefs()) {
        User user = userManager.getUser(userDef.identifier);
        if (user == null) {
          user = userManager.createUser(userDef.identifier, userDef.name);
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
  }

  private static class UserBootstrapConfigLoader implements BootstrapConfigLoader {
    private final List<UserDef> userDefs = new ArrayList<>();

    @Override
    public String getConfigElementName() {
      return "user_config";
    }

    @Override
    public DefaultHandler getDefaultHandler() {
      return new UserConfigHandler();
    }

    List<UserDef> getUserDefs() {
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
          userDef = new UserDef(attributes.getValue("identifier"), attributes.getValue("name"));
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

  private static class UserDef {
    private final String identifier;
    private final String name;
    private final Set<String> roleGrants = new HashSet<>();

    private UserDef(String identifier, String name) {
      this.identifier = identifier;
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
