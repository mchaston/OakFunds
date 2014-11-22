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
package org.chaston.oakfunds.model;

import org.chaston.oakfunds.storage.EnumIdentifiableSource;
import org.chaston.oakfunds.storage.Identifiable;
import org.chaston.oakfunds.storage.IdentifiableSource;

/**
 * TODO(mchaston): write JavaDocs
 */
public enum DistributionTimeUnit implements Identifiable {
  MONTHS {
    @Override
    public byte identifier() {
      return 1;
    }

    @Override
    public String toJson() {
      return "months";
    }
  },
  YEARS {
    @Override
    public byte identifier() {
      return 2;
    }

    @Override
    public String toJson() {
      return "years";
    }
  };

  private static final IdentifiableSource<DistributionTimeUnit> IDENTIFIABLE_SOURCE =
      new EnumIdentifiableSource<>(DistributionTimeUnit.class);

  /**
   * Supports the Identifiable type contract.
   */
  public static IdentifiableSource<DistributionTimeUnit> getIdentifiableSource() {
    return IDENTIFIABLE_SOURCE;
  }
}
