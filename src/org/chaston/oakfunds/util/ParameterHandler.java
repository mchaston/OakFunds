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

import com.google.common.collect.ImmutableList;
import org.chaston.oakfunds.storage.Identifiable;
import org.chaston.oakfunds.storage.IdentifiableSource;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

/**
 * TODO(mchaston): write JavaDocs
 */
public class ParameterHandler<T> {

  private static final DateTimeFormatter DATE_TIME_FORMAT = new DateTimeFormatterBuilder()
      .appendYear(4, 4)
      .appendLiteral('-').appendMonthOfYear(2)
      .appendLiteral('-').appendDayOfMonth(2)
      .appendLiteral('T')
      .appendHourOfDay(2)
      .appendLiteral(':').appendMinuteOfHour(2)
      .appendLiteral(':').appendSecondOfMinute(2)
      .appendLiteral('.').appendMillisOfSecond(3)
      .appendTimeZoneOffset("Z", true, 2, 4)
      .toFormatter();

  private final String parameter;
  private final StringValueParser<T> stringValueParser;
  private final JSONValueParser<T> jsonValueParser;
  private final String requiredMessage;
  private final boolean repeatedValue;
  private final T defaultValue;

  private ParameterHandler(
      String parameter,
      StringValueParser<T> stringValueParser,
      JSONValueParser<T> jsonValueParser,
      String requiredMessage,
      boolean repeatedValue,
      T defaultValue) {
    this.parameter = parameter;
    this.stringValueParser = stringValueParser;
    this.jsonValueParser = jsonValueParser;
    this.requiredMessage = requiredMessage;
    this.repeatedValue = repeatedValue;
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
    if (repeatedValue) {
      throw new IllegalStateException("Parameter " + parameter + " is a repeated value.");
    }
    Object parameterValue = jsonRequest.get(parameter);
    if (parameterValue == null) {
      if (requiredMessage != null) {
        throw new ServletException(requiredMessage);
      }
      return defaultValue;
    }
    if (parameterValue instanceof JSONArray) {
      throw new ServletException(
          "Parameter " + parameter
              + " was expected to be a single value, but an array was provided.");
    }
    return jsonValueParser.parse(parameterValue);
  }

  public List<T> parseRepeated(JSONObject jsonRequest) throws ServletException {
    if (!repeatedValue) {
      throw new IllegalStateException("Parameter " + parameter + " is not a repeated value.");
    }
    Object possibleArray = jsonRequest.get(parameter);
    if (possibleArray == null) {
      return ImmutableList.of();
    }
    if (!(possibleArray instanceof JSONArray)) {
      throw new ServletException(
          "Parameter " + parameter
              + " was expected to be a multi-value, but a single value was provided.");
    }
    JSONArray array = (JSONArray) possibleArray;
    ImmutableList.Builder<T> valuesBuilder = ImmutableList.builder();
    for (Object parameterValue : array) {
      valuesBuilder.add(jsonValueParser.parse(parameterValue));
    }
    return valuesBuilder.build();
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
            if (jsonValue instanceof Long) {
              return ((Long) jsonValue).intValue();
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

  public static ParameterHandler.Builder<Instant> instantParameter(final String parameter) {
    return new Builder<>(parameter,
        new StringValueParser<Instant>() {
          @Override
          public Instant parse(String stringValue) throws ServletException {
            try {
              return DATE_TIME_FORMAT.parseDateTime(stringValue).toInstant();
            } catch (IllegalArgumentException e) {
              throw new ServletException(
                  "Could not process date request parameter " + parameter + ": " + stringValue);
            }
          }
        },
        new JSONValueParser<Instant>() {
          @Override
          public Instant parse(Object jsonValue) throws ServletException {
            try {
              return DATE_TIME_FORMAT.parseDateTime((String) jsonValue).toInstant();
            } catch (IllegalArgumentException e) {
              throw new ServletException(
                  "Could not process date request parameter " + parameter + ": " + jsonValue);
            }
          }
        });
  }

  public static ParameterHandler.Builder<BigDecimal> bigDecimalParameter(final String parameter) {
    return new Builder<>(parameter,
        new StringValueParser<BigDecimal>() {
          @Override
          public BigDecimal parse(String stringValue) throws ServletException {
            return BigDecimal.valueOf(Double.parseDouble(stringValue));
          }
        },
        new JSONValueParser<BigDecimal>() {
          @Override
          public BigDecimal parse(Object jsonValue) throws ServletException {
            if (jsonValue instanceof Double) {
              return BigDecimal.valueOf((double) jsonValue);
            }
            if (jsonValue instanceof Long) {
              return BigDecimal.valueOf((long) jsonValue);
            }
            if (jsonValue instanceof Integer) {
              return BigDecimal.valueOf((int) jsonValue);
            }
            if (jsonValue instanceof String) {
              try {
                return BigDecimal.valueOf(Double.parseDouble((String) jsonValue));
              } catch (NumberFormatException e) {
                throw new ServletException(
                    "Could not process big double request attribute " + parameter
                        + ": " + jsonValue);
              }
            }
            throw new ServletException(
                "Could not process integer request attribute " + parameter + ": " + jsonValue);
          }
        });
  }

  public static <T extends Identifiable> Builder<T> identifiableParameter(final String parameter,
      final IdentifiableSource<T> identifiableSource) {
    return new Builder<>(parameter,
        new StringValueParser<T>() {
          @Override
          public T parse(String stringValue) throws ServletException {
            return identifiableSource.fromJson(stringValue);
          }
        },
        new JSONValueParser<T>() {
          @Override
          public T parse(Object jsonValue) throws ServletException {
            if (jsonValue instanceof String) {
              return identifiableSource.fromJson((String) jsonValue);
            }
            throw new ServletException(
                "Could not process " + identifiableSource.getTypeName() + " request attribute "
                    + parameter + ": " + jsonValue);
          }
        });
  }

  public static class Builder<T> {
    private final String parameter;
    private final StringValueParser<T> parserFunction;
    private final JSONValueParser<T> jsonValueParser;
    private String requiredMessage;
    private boolean repeatedValue;
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

    public Builder<T> repeatedValue() {
      this.repeatedValue = true;
      return this;
    }

    public Builder<T> withDefaultValue(T defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public ParameterHandler<T> build() {
      return new ParameterHandler<>(parameter, parserFunction, jsonValueParser, requiredMessage,
          repeatedValue, defaultValue);
    }
  }

  public interface StringValueParser<T> {
    T parse(String stringValue) throws ServletException;
  }

  public interface JSONValueParser<T> {
    T parse(Object jsonValue) throws ServletException;
  }
}
