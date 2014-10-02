package org.chaston.oakfunds.model;

import org.chaston.oakfunds.storage.Record;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public class Model extends Record {
  Model(int id) {
    super(RecordType.MODEL, id);
  }
}
