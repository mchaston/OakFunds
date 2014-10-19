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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

/**
 * TODO(mchaston): write JavaDocs
 */
@RunWith(JUnit4.class)
public class FlagValueProviderTest {
  @Test
  public void readFlagViaModuleBinding() {
    Injector injector = Guice.createInjector(new TestModule());

    assertEquals(44, injector.getInstance(NeedsInjection.class).getInjectedFlagValue());

    // Note: This would not work if NeedsInjection was a singleton.
    // This would *really* not work if NeedsInjection was an eager singleton.
    Flags.parse(new String[] { "--injectedFlag=99" });
    assertEquals(99, injector.getInstance(NeedsInjection.class).getInjectedFlagValue());
  }

  private static class TestModule extends AbstractModule {
    private final Flag<Integer> integerFlag = Flag.builder("injectedFlag", 44).build();

    @Override
    protected void configure() {
      bind(Integer.class).annotatedWith(Names.named("injectedFlag"))
          .toProvider(new FlagValueProvider<>(integerFlag));
    }
  }

  private static class NeedsInjection {
    private final int injectedFlagValue;

    @Inject
    NeedsInjection(@Named("injectedFlag") int injectedFlagValue) {
      this.injectedFlagValue = injectedFlagValue;
    }

    public int getInjectedFlagValue() {
      return injectedFlagValue;
    }
  }
}
