package org.chaston.oakfunds.account;

import org.chaston.oakfunds.storage.Attribute;
import org.chaston.oakfunds.storage.Record;
import org.chaston.oakfunds.storage.RecordType;

/**
 * TODO(mchaston): write JavaDocs
 */
public class AccountCode extends Record {

  @Attribute(name = "title")
  private String title;

  AccountCode(int id) {
    super(RecordType.ACCOUNT_CODE, id);
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
