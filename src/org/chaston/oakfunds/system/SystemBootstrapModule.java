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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.chaston.oakfunds.bootstrap.BootstrapConfigLoader;
import org.chaston.oakfunds.bootstrap.BootstrapTask;
import org.chaston.oakfunds.bootstrap.TransactionalBootstrapTask;
import org.chaston.oakfunds.security.AuthorizationContext;
import org.chaston.oakfunds.security.SinglePermissionAssertion;
import org.chaston.oakfunds.storage.AttributeSearchTerm;
import org.chaston.oakfunds.storage.SearchOperator;
import org.chaston.oakfunds.storage.SearchTerm;
import org.chaston.oakfunds.storage.Store;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class SystemBootstrapModule extends AbstractModule {
  @Override
  protected void configure() {
    requireBinding(Store.class);
    requireBinding(AuthorizationContext.class);
    install(new SystemModule());

    Multibinder<BootstrapConfigLoader> bootstrapConfigLoaderBinder =
        Multibinder.newSetBinder(binder(), BootstrapConfigLoader.class);
    bootstrapConfigLoaderBinder.addBinding().to(SystemBootstrapConfigLoader.class);
    bind(SystemBootstrapConfigLoader.class).in(Singleton.class);

    Multibinder<BootstrapTask> bootstrapTaskBinder =
        Multibinder.newSetBinder(binder(), BootstrapTask.class);
    bootstrapTaskBinder.addBinding().to(SystemBootstrapTask.class);
  }

  private static class SystemBootstrapTask extends TransactionalBootstrapTask {
    private final AuthorizationContext authorizationContext;
    private final SystemBootstrapConfigLoader systemBootstrapConfigLoader;

    @Inject
    SystemBootstrapTask(
        Store store,
        AuthorizationContext authorizationContext,
        SystemBootstrapConfigLoader systemBootstrapConfigLoader) {
      super(store);
      this.authorizationContext = authorizationContext;
      this.systemBootstrapConfigLoader = systemBootstrapConfigLoader;
    }

    @Override
    protected void bootstrapDuringTransaction() throws Exception {
      try (SinglePermissionAssertion singlePermissionAssertion =
               authorizationContext.assertPermission("system_property.create")) {
        for (SystemPropertyDef systemPropertyDef : systemBootstrapConfigLoader.getUserDefs()) {
          List<? extends SearchTerm> searchTerms = ImmutableList.of(
              AttributeSearchTerm.of(SystemProperty.ATTRIBUTE_NAME,
                  SearchOperator.EQUALS, systemPropertyDef.name));
          Iterable<SystemProperty> existingValues =
              getStore().findRecords(SystemProperty.TYPE, searchTerms);
          if (Iterables.isEmpty(existingValues)) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(SystemProperty.ATTRIBUTE_NAME, systemPropertyDef.name);
            attributes.putAll(systemPropertyDef.getOtherAttributes());
            getStore().createRecord(SystemProperty.TYPE, attributes);
          }
        }
      }
    }
  }

  private static class SystemBootstrapConfigLoader implements BootstrapConfigLoader {
    private final List<SystemPropertyDef> systemPropertyDefs = new ArrayList<>();

    @Override
    public String getConfigElementName() {
      return "system_config";
    }

    @Override
    public DefaultHandler getDefaultHandler() {
      return new SystemConfigHandler();
    }

    List<SystemPropertyDef> getUserDefs() {
      return systemPropertyDefs;
    }

    private class SystemConfigHandler extends DefaultHandler {
      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes)
          throws SAXException {
        if (qName.equals("integer_system_property")) {
          systemPropertyDefs.add(new IntegerSystemPropertyDef(attributes.getValue("name"),
              Integer.parseInt(attributes.getValue("value"))));
        } else {
          throw new SAXException("Element " + qName + " not supported.");
        }
      }
    }
  }

  private abstract static class SystemPropertyDef {
    private final String name;

    protected SystemPropertyDef(String name) {
      this.name = name;
    }

    public abstract Map<String, Object> getOtherAttributes();
  }

  private static class IntegerSystemPropertyDef extends SystemPropertyDef {
    private final int value;

    private IntegerSystemPropertyDef(String name, int value) {
      super(name);
      this.value = value;
    }

    @Override
    public Map<String, Object> getOtherAttributes() {
      return ImmutableMap.<String, Object>of(SystemProperty.ATTRIBUTE_INTEGER_VALUE, value);
    }
  }
}
