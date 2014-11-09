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

import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ParameterHandler<T> {
  private final String parameter;
  private final StringValueParser<T> stringValueParser;
  private final JSONValueParser<T> jsonValueParser;
  private final String requiredMessage;
  private final T defaultValue;

  private ParameterHandler(
      String parameter,
      StringValueParser<T> stringValueParser,
      JSONValueParser<T> jsonValueParser, String requiredMessage,
      T defaultValue) {
    this.parameter = parameter;
    this.stringValueParser = stringValueParser;
    this.jsonValueParser = jsonValueParser;
    this.requiredMessage = requiredMessage;
    this.defaultValue = defaultValue;
  }

  public T parse(HttpServletRequest request) throws ServletException {
    String[] stringValues = request.getParameterValues(parameter);
    if (stringValues == null) {
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
    return stringValueParser.parse(stringValues[0]);
  }

  public T parse(JSONObject jsonRequest) throws ServletException {
    Object parameterValue = jsonRequest.get(parameter);
    if (parameterValue == null) {
      if (requiredMessage != null) {
        throw new ServletException(requiredMessage);
      }
      return defaultValue;
    }
    return jsonValueParser.parse(parameterValue);
  }

  public static ParameterHandler.Builder<Integer> intParameter(final String parameter) {
    return new Builder<>(parameter,
        new StringValueParser<Integer>() {
          @Override
          public Integer parse(String stringValue) throws ServletException {
            try {
              return Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
              throw new ServletException(
                  "Could not process integer request parameter " + parameter + ": " + stringValue);
            }
          }
        },
        new JSONValueParser<Integer>() {
          @Override
          public Integer parse(Object jsonValue) throws ServletException {
            if (jsonValue instanceof Integer) {
              return (Integer) jsonValue;
            }
            if (jsonValue instanceof String) {
              try {
                return Integer.parseInt((String) jsonValue);
              } catch (NumberFormatException e) {
                throw new ServletException(
                    "Could not process integer request attribute " + parameter + ": " + jsonValue);
              }
            }
            throw new ServletException(
                "Could not process integer request attribute " + parameter + ": " + jsonValue);
          }
        });
  }

  public static ParameterHandler.Builder<String> stringParameter(final String parameter) {
    return new Builder<>(parameter,
        new StringValueParser<String>() {
          @Override
          public String parse(String stringValue) throws ServletException {
            return stringValue;
          }
        },
        new JSONValueParser<String>() {
          @Override
          public String parse(Object jsonValue) throws ServletException {
            return (String) jsonValue;
          }
        });
  }

  public static class Builder<T> {
    private final String parameter;
    private final StringValueParser<T> parserFunction;
    private final JSONValueParser<T> jsonValueParser;
    private String requiredMessage;
    private T defaultValue;

    Builder(String parameter,
        StringValueParser<T> parserFunction,
        JSONValueParser<T> jsonValueParser) {
      this.parameter = parameter;
      this.parserFunction = parserFunction;
      this.jsonValueParser = jsonValueParser;
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
      return new ParameterHandler<>(parameter, parserFunction, jsonValueParser, requiredMessage, defaultValue);
    }
  }

  public interface StringValueParser<T> {
    T parse(String stringValue) throws ServletException;
  }

  public interface JSONValueParser<T> {
    T parse(Object jsonValue) throws ServletException;
  }
}
