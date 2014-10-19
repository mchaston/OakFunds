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

import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

/**
 * TODO(mchaston): write JavaDocs
 */
public class Flag<V> {

  private final String name;
  private final FlagValueParser<V> parser;
  private final V defaultValue;
  private boolean set;
  private V value;
  private String shortName;

  protected Flag(String name, String shortName, FlagValueParser<V> parser, V defaultValue) {
    this.name = name;
    this.shortName = shortName;
    this.parser = parser;
    this.defaultValue = defaultValue;
    Flags.registerFlag(this);
  }

  @Nullable
  public V get() {
    if (set) {
      return value;
    } else {
      return defaultValue;
    }
  }

  void parse(String value) {
    this.value = parser.parse(value);
    this.set = true;
  }

  public String getName() {
    return name;
  }

  public String getShortName() {
    return shortName;
  }

  public static Builder<Integer> builder(String name, int defaultValue) {
    return builder(name, new IntegerFlagValueParser(), defaultValue);
  }

  public static Builder<String> builder(String name, String defaultValue) {
    return builder(name, new StringFlagValueParser(), defaultValue);
  }

  public static Builder<Boolean> builder(String name, boolean defaultValue) {
    return builder(name, new BooleanFlagValueParser(), defaultValue);
  }

  public static <V> Builder<V> builder(String name, FlagValueParser<V> parser) {
    return builder(name, parser, null);
  }

  public static <V> Builder<V> builder(String name, FlagValueParser<V> parser, V defaultValue) {
    return new Builder<>(name, parser, defaultValue);
  }

  public static class Builder<V> {
    private final String name;
    private final FlagValueParser<V> parser;
    private final V defaultValue;
    private String shortName;

    private Builder(String name, FlagValueParser<V> parser, V defaultValue) {
      this.name = Preconditions.checkNotNull(name, "name");
      this.parser = Preconditions.checkNotNull(parser, "parser");
      this.defaultValue = defaultValue;
    }

    public Builder<V> setShortName(String shortName) {
      this.shortName = shortName;
      return this;
    }

    public Flag<V> build() {
      return new Flag<>(name, shortName, parser, defaultValue);
    }
  }

  private static class IntegerFlagValueParser implements FlagValueParser<Integer> {
    @Override
    public Integer parse(String value) {
      if (value == null || value.isEmpty()) {
        return null;
      }
      return Integer.parseInt(value);
    }
  }

  private static class StringFlagValueParser implements FlagValueParser<String> {
    @Override
    public String parse(String value) {
      return value;
    }
  }

  private static class BooleanFlagValueParser implements FlagValueParser<Boolean> {
    @Override
    public Boolean parse(String value) {
      if (value == null || value.isEmpty()) {
        return null;
      }
      return Boolean.parseBoolean(value);
    }
  }
}
