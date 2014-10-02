package org.chaston.oakfunds.storage;

/**
 * TODO(mchaston): write JavaDocs
 */
public abstract class Record {
  private final int id;
  private final RecordType recordType;

  protected Record(RecordType recordType, int id) {
    this.recordType = recordType;
    this.id = id;
  }

  public int getId() {
    return id;
  }

  RecordType getRecordType() {
    return recordType;
  }
}
