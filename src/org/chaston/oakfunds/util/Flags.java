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
package org.chaston.oakfunds.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
public class Flags {

  private static final Map<String, Flag> flagsByName = new HashMap<>();
  private static final Map<String, Flag> flagsByShortName = new HashMap<>();

  public static String[] parse(String[] args) {
    List<String> remainingArgs = new ArrayList<>();
    for (String arg : args) {
      int equalsIndex = arg.indexOf('=');
      Flag flag;
      if (arg.startsWith("--")) {
        if (equalsIndex == -1) {
          throw new IllegalArgumentException(
              "Flag argument did not contain '=' character: " + arg);
        }
        String flagName = arg.substring(2, equalsIndex);
        flag = flagsByName.get(flagName);
        if (flag == null) {
          throw new IllegalArgumentException("Flag " + flagName + " was not bound.");
        }
      } else if (arg.startsWith("-")) {
        if (equalsIndex == -1) {
          throw new IllegalArgumentException(
              "Flag argument did not contain '=' character: " + arg);
        }
        String flagName = arg.substring(1, equalsIndex);
        flag = flagsByShortName.get(flagName);
        if (flag == null) {
          throw new IllegalArgumentException("Flag " + flagName + " was not bound.");
        }
      } else {
        // If not a flag, just skip it.
        remainingArgs.add(arg);
        continue;
      }
      String flagValue = arg.substring(equalsIndex + 1);
      flag.parse(flagValue);
    }
    return remainingArgs.toArray(new String[remainingArgs.size()]);
  }

  static <V> void registerFlag(Flag<V> flag) {
    if (flagsByName.containsKey(flag.getName())) {
      throw new IllegalStateException(
          "Multiple flags bound with the same name: " + flag.getName());
    }
    if (flag.getShortName() != null) {
      if (flagsByShortName.containsKey(flag.getShortName())) {
        throw new IllegalStateException(
            "Multiple flags bound with the same short name: " + flag.getShortName());
      }
    }
    flagsByName.put(flag.getName(), flag);
    if (flag.getShortName() != null) {
      flagsByShortName.put(flag.getShortName(), flag);
    }
  }
}
