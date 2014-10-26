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
package org.chaston.oakfunds.bootstrap;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * TODO(mchaston): write JavaDocs
 */
class BootstrapConfigReader {

  private final ImmutableMap<String, BootstrapConfigLoader> configLoaders;

  @Inject
  BootstrapConfigReader(Set<BootstrapConfigLoader> configLoaders) {
    ImmutableMap.Builder<String, BootstrapConfigLoader> configLoadersBuilder =
        ImmutableMap.builder();
    for (BootstrapConfigLoader configLoader : configLoaders) {
      configLoadersBuilder.put(configLoader.getConfigElementName(), configLoader);
    }
    this.configLoaders = configLoadersBuilder.build();
  }

  public void read(File configFile) throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    SAXParser saxParser = saxParserFactory.newSAXParser();
    saxParser.parse(configFile, new BaseConfigHandler());
  }

  private class BaseConfigHandler extends DefaultHandler {
    private String currentElement;
    private DefaultHandler subHandler;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      if (subHandler != null) {
        subHandler.startElement(uri, localName, qName, attributes);
      } else if (qName.equals("boostrap_config")) {
        // Known wrapping element.
      } else {
        BootstrapConfigLoader configLoader = configLoaders.get(qName);
        if (configLoader == null) {
          throw new SAXException("No handler for element " + qName + " was found.");
        }
        currentElement = qName;
        subHandler = configLoader.getDefaultHandler();
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (subHandler != null) {
        subHandler.characters(ch, start, length);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (currentElement != null && currentElement.equals(qName)) {
        currentElement = null;
        subHandler = null;
      }
    }
  }
}
