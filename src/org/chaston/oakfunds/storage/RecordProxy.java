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
package org.chaston.oakfunds.storage;

import org.joda.time.Instant;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * TODO(mchaston): write JavaDocs
 */
class RecordProxy {
  static <T extends Record> T proxyRecord(
      RecordType<T> recordType, @Nullable Record<?> parent, int id, Map<String, Object> attributes) {
    return (T) Proxy.newProxyInstance(
        recordType.getRecordTypeClass().getClassLoader(),
        new Class[] { recordType.getRecordTypeClass() },
        new RecordProxyInvocationHandler(recordType, parent, id, attributes));
  }

  static <T extends InstantRecord> T proxyInstantRecord(
      RecordType<T> recordType, @Nullable Record<?> parent, int id, Instant instant, Map<String, Object> attributes) {
    return (T) Proxy.newProxyInstance(
        recordType.getRecordTypeClass().getClassLoader(),
        new Class[] { recordType.getRecordTypeClass() },
        new InstantRecordProxyInvocationHandler(recordType, parent, id, instant, attributes));
  }

  static <T extends IntervalRecord> T proxyIntervalRecord(
      RecordType<T> recordType, @Nullable Record<?> parent, int id, Instant start, Instant end, Map<String, Object> attributes) {
    return (T) Proxy.newProxyInstance(
        recordType.getRecordTypeClass().getClassLoader(),
        new Class[] { recordType.getRecordTypeClass() },
        new IntervalRecordProxyInvocationHandler(recordType, parent, id, start, end, attributes));
  }

  private static class RecordProxyInvocationHandler<T extends Record> implements InvocationHandler {
    private final RecordType<T> recordType;
    @Nullable
    private final Integer parentId;
    private final int id;
    private final Map<String, Object> attributes;

    RecordProxyInvocationHandler(
        RecordType<T> recordType, @Nullable Record<?> parent, int id, Map<String, Object> attributes) {
      this.recordType = recordType;
      this.parentId = parent == null ? null : parent.getId();
      this.id = id;
      this.attributes = attributes;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("getRecordType")) {
        return recordType;
      }
      if (method.getName().equals("getId")) {
        return id;
      }
      ParentIdMethod parentIdMethod = method.getAnnotation(ParentIdMethod.class);
      if (parentIdMethod != null) {
        return parentId;
      }
      AttributeMethod attributeMethod = method.getAnnotation(AttributeMethod.class);
      if (attributeMethod != null) {
        return attributes.get(attributeMethod.attribute());
      }
      if (method.getName().equals("toString") && method.getReturnType() == String.class) {
        return recordType.getName() + ":" + id;
      }
      Object otherReturnValue = getOtherReturnValue(method);
      if (otherReturnValue == null) {
        throw new IllegalStateException("Method " + method.getName()
            + " was called on " + recordType.getRecordTypeClass() + " but was not supported.");
      }
      return otherReturnValue;
    }

    Object getOtherReturnValue(Method method) {
      return null;
    }
  }

  private static class InstantRecordProxyInvocationHandler<T extends InstantRecord>
      extends RecordProxyInvocationHandler<T> implements InvocationHandler {
    private final Instant instant;

    InstantRecordProxyInvocationHandler(
        RecordType<T> recordType, @Nullable Record<?> parent, int id, Instant instant,
        Map<String, Object> attributes) {
      super(recordType, parent, id, attributes);
      this.instant = instant;
    }

    @Override
    Object getOtherReturnValue(Method method) {
      if (method.getName().equals("getInstant")) {
        return instant;
      }
      return null;
    }
  }

  private static class IntervalRecordProxyInvocationHandler<T extends IntervalRecord>
      extends RecordProxyInvocationHandler<T> implements InvocationHandler {
    private final Instant start;
    private final Instant end;

    IntervalRecordProxyInvocationHandler(
        RecordType<T> recordType, @Nullable Record<?> parent, int id, Instant start, Instant end,
        Map<String, Object> attributes) {
      super(recordType, parent, id, attributes);
      this.start = start;
      this.end = end;
    }

    @Override
    Object getOtherReturnValue(Method method) {
      if (method.getName().equals("getStart")) {
        return start;
      }
      if (method.getName().equals("getEnd")) {
        return end;
      }
      return null;
    }
  }
}
