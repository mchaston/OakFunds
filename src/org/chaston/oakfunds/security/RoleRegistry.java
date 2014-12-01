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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.Nullable;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * TODO(mchaston): write JavaDocs
 */
class RoleRegistry {

  private static final Logger logger = Logger.getLogger(RoleRegistry.class.getName());

  private final ImmutableMap<String, Role> roles;

  @Inject
  RoleRegistry(PermissionRegistry permissionRegistry) throws Exception {
    // read these from roles.xml
    InputStream roleFileStream = getClass().getResourceAsStream("roles.xml");
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    SAXParser saxParser = saxParserFactory.newSAXParser();
    RoleBuilder rolesBuilder = new RoleBuilder(permissionRegistry);
    saxParser.parse(roleFileStream, rolesBuilder);

    this.roles = rolesBuilder.getRoles();
  }

  @Nullable
  public Role getRole(String roleName) {
    return roles.get(roleName);
  }

  public Iterable<Role> getRoles() {
    return roles.values();
  }

  private class RoleBuilder extends DefaultHandler {
    private final ImmutableMap.Builder<String, Role> rolesBuilder = ImmutableMap.builder();
    private final PermissionRegistry permissionRegistry;
    private Role.Builder currentRoleBuilder;

    RoleBuilder(PermissionRegistry permissionRegistry) {
      this.permissionRegistry = permissionRegistry;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      if (qName.equals("roles")) {
        // This is the outer element, so we can ignore it.
      } else if (qName.equals("role")) {
        if (currentRoleBuilder != null) {
          throw new IllegalStateException("New role element before the old one ended.");
        }
        currentRoleBuilder = Role.builder(attributes.getValue("name"));
      } else if (qName.equals("permission")) {
        if (currentRoleBuilder == null) {
          throw new SAXException("Element permission found outside role element. ");
        }
        Permission permission = permissionRegistry.getPermission(attributes.getValue("name"));
        if (permission == null) {
          logger.warning("Role " + currentRoleBuilder.getName()
              + " contained an unknown permission: " + attributes.getValue("name"));
        } else {
          currentRoleBuilder.addPermission(permission.getName());
        }
      } else {
        throw new SAXException("Unexpected element: " + qName);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (qName.equals("role")) {
        Role role = currentRoleBuilder.build();
        rolesBuilder.put(role.getName(), role);
        currentRoleBuilder = null;
      }
    }

    ImmutableMap<String, Role> getRoles() throws SAXException {
      if (currentRoleBuilder != null) {
        throw new SAXException("Role element was not closed.");
      }
      return rolesBuilder.build();
    }
  }
}
