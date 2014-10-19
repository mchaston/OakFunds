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
package org.chaston.oakfunds.jdbc;

import com.google.common.base.Charsets;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * TODO(mchaston): write JavaDocs
 */
public class FunctionDef {
  private final String name;
  private final String hsqldbFunction;
  private final String mysqlFunction;

  public FunctionDef(String name,
      String hsqldbResourcePath,
      String mysqlResourcePath) throws IOException {
    this.name = name;
    this.hsqldbFunction = loadResource(hsqldbResourcePath);
    this.mysqlFunction = loadResource(mysqlResourcePath);
  }

  private static String loadResource(String resourcePath) throws IOException {
    try (InputStream resourceStream = FunctionDef.class.getClassLoader()
        .getResourceAsStream(resourcePath)) {
      if (resourceStream == null) {
        throw new IllegalArgumentException(
            "Resource path " + resourcePath + " did not have any content.");
      }
      try (InputStreamReader in = new InputStreamReader(resourceStream, Charsets.UTF_8)) {
        try (CharArrayWriter out = new CharArrayWriter()) {
          char[] buf = new char[1024];
          int len = in.read(buf);
          while (len > -1) {
            out.write(buf, 0, len);
            len = in.read();
          }
          return out.toString();
        }
      }
    }
  }

  public String getName() {
    return name;
  }

  public String getHsqldbFunction() {
    return hsqldbFunction;
  }

  public String getMysqlFunction() {
    return mysqlFunction;
  }
}
