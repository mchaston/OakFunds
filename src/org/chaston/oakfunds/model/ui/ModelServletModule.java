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
package org.chaston.oakfunds.model.ui;

import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;

/**
* TODO(mchaston): write JavaDocs
*/
public class ModelServletModule extends ServletModule {
  @Override
  protected void configureServlets() {
    serve("/model/model/create").with(ModelCreateServlet.class);
    bind(ModelCreateServlet.class).in(Singleton.class);
    serve("/model/models").with(ModelListServlet.class);
    bind(ModelListServlet.class).in(Singleton.class);
    serveRegex(ModelUpdateServlet.URI_REGEX).with(ModelUpdateServlet.class);
    bind(ModelUpdateServlet.class).in(Singleton.class);
  }
}
