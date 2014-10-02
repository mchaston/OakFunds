package org.chaston.oakfunds.storage;

/**
 * TODO(mchaston): write JavaDocs
 */
public enum RecordType {
  ACCOUNT_CODE(RecordTemporalType.NONE, true),
  ACCOUNT(RecordTemporalType.NONE, false),
  BANK_ACCOUNT(ACCOUNT, true),
  EXPENSE_ACCOUNT(ACCOUNT, true),
  REVENUE_ACCOUNT(ACCOUNT, true),
  MODEL(RecordTemporalType.NONE, true),
  BANK_ACCOUNT_INTEREST(RecordTemporalType.INTERVAL, true),
  ACCOUNT_TRANSACTION(RecordTemporalType.INSTANT, true);

  private final RecordTemporalType temporalType;
  private final RecordType parentType;
  private final boolean isFinalType;

  RecordType(RecordType baseType, boolean isFinalType) {
    this.temporalType = baseType.getRootType().getTemporalType();
    this.parentType = baseType;
    this.isFinalType = isFinalType;
  }

  RecordType(RecordTemporalType temporalType, boolean isFinalType) {
    this.temporalType = temporalType;
    this.parentType = null;
    this.isFinalType = isFinalType;
  }

  public RecordTemporalType getTemporalType() {
    return temporalType;
  }

  public RecordType getRootType() {
    if (parentType != null) {
      return parentType.getRootType();
    }
    return this;
  }

  public boolean isFinalType() {
    return isFinalType;
  }
}
