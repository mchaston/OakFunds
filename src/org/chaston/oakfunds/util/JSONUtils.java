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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * TODO(mchaston): write JavaDocs
 */
public class JSONUtils {
  public static void writeJSONString(PrintWriter writer,
      Iterable<? extends JSONRepresentable> jsonRepresentables) throws IOException {
    JSONArray jsonArray = new JSONArray();
    for (JSONRepresentable record : jsonRepresentables) {
      jsonArray.add(record.toJSONObject());
    }
    jsonArray.writeJSONString(writer);
  }

  public static void writeJSONString(PrintWriter writer, JSONRepresentable jsonRepresentable)
      throws IOException {
    jsonRepresentable.toJSONObject().writeJSONString(writer);
  }

  public static JSONObject readRequest(HttpServletRequest request, String requestType)
      throws ServletException, IOException {
    ServletInputStream in = request.getInputStream();
    InputStreamReader reader = new InputStreamReader(in, request.getCharacterEncoding());
    try {
      return  (JSONObject) new JSONParser().parse(reader);
    } catch (ParseException e) {
      throw new ServletException("Failure to parse " + requestType + " request.", e);
    }
  }
}
