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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ParameterHandler<T> {
  private final String parameter;
  private final ValueParser<T> parserFunction;
  private final String requiredMessage;
  private final T defaultValue;

  private ParameterHandler(
      String parameter,
      ValueParser<T> parserFunction,
      String requiredMessage,
      T defaultValue) {
    this.parameter = parameter;
    this.parserFunction = parserFunction;
    this.requiredMessage = requiredMessage;
    this.defaultValue = defaultValue;
  }

  public T parse(HttpServletRequest request) throws ServletException {
    String[] stringValues = request.getParameterValues(parameter);
    if (stringValues.length == 0) {
      if (requiredMessage != null) {
        throw new ServletException(requiredMessage);
      }
      return defaultValue;
    }
    if (stringValues.length > 1) {
      throw new ServletException(
          "Multiple values for parameter " + parameter
              + " was provided, but multi-value is not supported.");
    }
    return parserFunction.parse(stringValues[0]);
  }

  public static ParameterHandler.Builder<Integer> intParameter(final String parameter) {
    return new Builder<>(parameter,
        new ValueParser<Integer>() {
          @Override
          public Integer parse(String stringValue) throws ServletException {
            try {
              return Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
              throw new ServletException(
                  "Integer parameter " + parameter + " could not process value: " + stringValue);
            }
          }
        });
  }

  public static ParameterHandler.Builder<String> stringParameter(final String parameter) {
    return new Builder<>(parameter,
        new ValueParser<String>() {
          @Override
          public String parse(String stringValue) throws ServletException {
            return stringValue;
          }
        });
  }

  public static class Builder<T> {
    private final String parameter;
    private final ValueParser<T> parserFunction;
    private String requiredMessage;
    private T defaultValue;

    Builder(String parameter, ValueParser<T> parserFunction) {
      this.parameter = parameter;
      this.parserFunction = parserFunction;
    }

    public Builder<T> required(String requiredMessage) {
      this.requiredMessage = requiredMessage;
      return this;
    }

    public Builder<T> withDefaultValue(T defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public ParameterHandler<T> build() {
      return new ParameterHandler<>(parameter, parserFunction, requiredMessage, defaultValue);
    }
  }

  public interface ValueParser<T> {
    T parse(String stringValue) throws ServletException;
  }
}
