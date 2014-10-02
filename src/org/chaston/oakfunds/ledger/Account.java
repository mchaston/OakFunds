package org.chaston.oakfunds.ledger;

import org.chaston.oakfunds.storage.Attribute;
import org.chaston.oakfunds.storage.Record;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public abstract class Account extends Record {

  @Attribute(name = "title")
  private String title;

  Account(RecordType recordType, int id) {
    super(recordType, id);
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
