/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.requestfactory.client.SyncResultImpl;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.requestfactory.shared.WriteOperation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Accumulates the local edits, made in the context of a
 * {@link com.google.gwt.requestfactory.shared.RequestObject}.
 * 
 */
class DeltaValueStoreJsonImpl {

  static class ReturnRecord extends JavaScriptObject {

    public static final native JsArray<ReturnRecord> getRecords(
        JavaScriptObject response, String operation) /*-{
      return response[operation];
    }-*/;

    private static native void fillKeys(JavaScriptObject jso, HashSet<String> s) /*-{
      for (key in jso) {
        if (jso.hasOwnProperty(key)) {
          s.@java.util.HashSet::add(Ljava/lang/Object;)(key);
        }
      }
    }-*/;

    protected ReturnRecord() {
    }

    public final native void fillViolations(HashMap<String, String> s) /*-{
      for (key in this.violations) {
        if (this.violations.hasOwnProperty(key)) {
          s.@java.util.HashMap::put(Ljava/lang/Object;Ljava/lang/Object;)(key, this.violations[key]);
        }
      }
    }-*/;

    public final native String getFutureId()/*-{
      return this.futureId;
    }-*/;

    public final Long getId() {
      String parts[] = getSchemaAndId().split("-");
      return Long.parseLong(parts[1]);
    }

    public final String getSchema() {
      String parts[] = getSchemaAndId().split("-");
      return parts[0];
    }

    public final native String getSchemaAndId() /*-{
      return this.id;
    }-*/;

    public final native String getVersion()/*-{
      return this.version;
    }-*/;

    public final native boolean hasFutureId()/*-{
      return 'futureId' in this;
    }-*/;

    public final native boolean hasId()/*-{
      return 'id' in this;
    }-*/;

    public final native boolean hasViolations()/*-{
      return 'violations' in this;
    }-*/;
  }

  private static final HashMap<String, String> NULL_VIOLATIONS = new HashMap<String, String>();

  private boolean used = false;

  private final ValueStoreJsonImpl master;
  private final RequestFactoryJsonImpl requestFactory;

  // track C-U-D of CRUD operations
  private final Map<EntityProxyId, ProxyJsoImpl> creates = new HashMap<EntityProxyId, ProxyJsoImpl>();
  private final Map<EntityProxyId, ProxyJsoImpl> updates = new HashMap<EntityProxyId, ProxyJsoImpl>();
  // nothing for deletes because DeltaValueStore is not involved in deletes. The
  // operation alone suffices.

  private final Map<EntityProxyId, WriteOperation> operations = new HashMap<EntityProxyId, WriteOperation>();

  public DeltaValueStoreJsonImpl(ValueStoreJsonImpl master,
      RequestFactoryJsonImpl requestFactory) {
    this.master = master;
    this.requestFactory = requestFactory;
  }

  public void addValidation() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  public void clearUsed() {
    used = false;
  }

  public Set<SyncResult> commit(JavaScriptObject returnedJso) {
    Set<SyncResult> syncResults = new HashSet<SyncResult>();
    HashSet<String> keys = new HashSet<String>();
    ReturnRecord.fillKeys(returnedJso, keys);

    Set<EntityProxyId> toRemove = new HashSet<EntityProxyId>();
    if (keys.contains(WriteOperation.CREATE.name())) {
      JsArray<ReturnRecord> newRecords = ReturnRecord.getRecords(returnedJso,
          WriteOperation.CREATE.name());
      /*
       * construct 2 maps: (i) futureId to the datastore Id, (ii) futureId to
       * violationsMap
       */
      Map<Long, Long> futureToDatastoreId = new HashMap<Long, Long>();
      Map<Long, Map<String, String>> violationsMap = new HashMap<Long, Map<String, String>>();
      int length = newRecords.length();
      for (int i = 0; i < length; i++) {
        ReturnRecord sync = newRecords.get(i);
        if (sync.hasViolations()) {
          // does not have an id.
          assert !sync.hasId();
          HashMap<String, String> violations = new HashMap<String, String>();
          sync.fillViolations(violations);
          violationsMap.put(Long.valueOf(sync.getFutureId()), violations);
        } else {
          violationsMap.put(Long.valueOf(sync.getFutureId()), NULL_VIOLATIONS);
          futureToDatastoreId.put(Long.valueOf(sync.getFutureId()),
              sync.getId());
        }
      }

      for (Map.Entry<EntityProxyId, ProxyJsoImpl> entry : creates.entrySet()) {
        final EntityProxyIdImpl futureKey = (EntityProxyIdImpl) entry.getKey();
        // TODO change violationsMap to <Long, String>
        Map<String, String> violations = violationsMap.get(futureKey.id);
        assert violations != null;
        if (violations == NULL_VIOLATIONS) {
          Long datastoreId = futureToDatastoreId.get(futureKey.id);
          assert datastoreId != null;
          
          // populate the two maps first.
          requestFactory.datastoreToFutureMap.put(datastoreId, futureKey.schema, futureKey.id);
          requestFactory.futureToDatastoreMap.put(futureKey.id, datastoreId);
          final EntityProxyIdImpl key = getPersistedProxyId(datastoreId, futureKey.schema);
          ProxyJsoImpl value = entry.getValue();

          // make a copy of value and set the id there.
          // TODO (amitmanjhi): get all the data from the server.
          ProxyJsoImpl copy = ProxyJsoImpl.emptyCopy(value);
          copy.merge(value);
          copy.set(EntityProxy.id, datastoreId);

          ProxyJsoImpl masterRecord = master.records.get(key);
          assert masterRecord == null;

          masterRecord = copy;
          toRemove.add(key);
          requestFactory.postChangeEvent(masterRecord, WriteOperation.CREATE);
          syncResults.add(makeSyncResult(masterRecord, null, futureKey.id));
        } else {
          // do not change the masterRecord or fire event
          syncResults.add(makeSyncResult(entry.getValue(), violations,
              futureKey.id));
        }
      }
    }
    processToRemove(toRemove, WriteOperation.CREATE);
    toRemove.clear();

    if (keys.contains(WriteOperation.DELETE.name())) {
      JsArray<ReturnRecord> deletedRecords = ReturnRecord.getRecords(
          returnedJso, WriteOperation.DELETE.name());
      Map<Long, Map<String, String>> violationsMap = getViolationsMap(deletedRecords);
      int length = deletedRecords.length();

      for (int i = 0; i < length; i++) {
        final EntityProxyIdImpl key = getPersistedProxyId(deletedRecords.get(i).getId(),
            requestFactory.getSchema(deletedRecords.get(i).getSchema()));
        Map<String, String> violations = violationsMap.get(key.id);
        assert violations != null;
        /*
         * post change event if no violations.
         *
         * TODO: there needs to be a separate path for violations, not mingled
         * with update events.
         */
        if (violations == NULL_VIOLATIONS) {
          requestFactory.postChangeEvent(ProxyJsoImpl.create((Long) key.id, 1,
              key.schema, requestFactory), WriteOperation.DELETE);
        }
        ProxyJsoImpl masterRecord = master.records.get(key);
        if (masterRecord != null) {
          master.records.remove(key);
          syncResults.add(makeSyncResult(masterRecord, null, null));
        } else {
          syncResults.add(makeSyncResult(masterRecord, violations, null));
        }
      }
    }

    if (keys.contains(WriteOperation.UPDATE.name())) {
      JsArray<ReturnRecord> updatedRecords = ReturnRecord.getRecords(
          returnedJso, WriteOperation.UPDATE.name());
      Map<Long, Map<String, String>> violationsMap = getViolationsMap(updatedRecords);

      int length = updatedRecords.length();
      for (int i = 0; i < length; i++) {
        final EntityProxyIdImpl key = getPersistedProxyId(updatedRecords.get(i).getId(),
            requestFactory.getSchema(updatedRecords.get(i).getSchema()));
        Map<String, String> violations = violationsMap.get(key.id);
        assert violations != null;
        // post change events if no violations.
        if (violations == NULL_VIOLATIONS) {
          requestFactory.postChangeEvent(ProxyJsoImpl.create((Long) key.id, 1,
              key.schema, requestFactory), WriteOperation.UPDATE);
        }

        ProxyJsoImpl masterRecord = master.records.get(key);
        ProxyJsoImpl value = updates.get(key);
        if (masterRecord != null && value != null) {
          // no support for partial updates.
          masterRecord.merge(value);
          toRemove.add(key);
        }
        if (masterRecord != null) {
          syncResults.add(makeSyncResult(masterRecord, null, null));
        } else {
          // do not change the masterRecord or fire event
          syncResults.add(makeSyncResult(masterRecord, violations, null));
        }
      }
    }
    processToRemove(toRemove, WriteOperation.UPDATE);
    return syncResults;
  }

  public boolean isChanged() {
    assert !used;
    return !operations.isEmpty();
  }

  public <V> void set(Property<V> property, EntityProxy record, V value) {
    checkArgumentsAndState(record, "set");
    ProxyImpl recordImpl = (ProxyImpl) record;
    EntityProxyId recordKey = recordImpl.getStableId();

    ProxyJsoImpl rawMasterRecord = master.records.get(recordKey);
    WriteOperation priorOperation = operations.get(recordKey);
    if (rawMasterRecord == null && priorOperation == null) {
      operations.put(recordKey, WriteOperation.CREATE);
      creates.put(recordKey, recordImpl.asJso());
      priorOperation = WriteOperation.CREATE;
    }
    if (priorOperation == null) {
      addNewChangeRecord(recordKey, recordImpl, property, value);
      return;
    }

    ProxyJsoImpl priorRecord = null;
    switch (priorOperation) {
      case CREATE:
        // nothing to do here.
        priorRecord = creates.get(recordKey);
        assert priorRecord != null;
        priorRecord.set(property, value);
        break;
      case UPDATE:
        priorRecord = updates.get(recordKey);
        assert priorRecord != null;

        if (isRealChange(property, value, rawMasterRecord)) {
          priorRecord.set(property, value);
          updates.put(recordKey, priorRecord);
          return;
        }
        /*
         * Not done yet. If the user has changed the value back to the original
         * value, we should eliminate the previous value from the changeRecord.
         * And if the changeRecord is now empty, we should drop it entirely.
         */

        if (priorRecord.isDefined(property.getName())) {
          priorRecord.delete(property.getName());
        }
        if (updates.containsKey(recordKey) && priorRecord.isEmpty()) {
          updates.remove(recordKey);
          operations.remove(recordKey);
        }
        break;
    }
  }

  String toJson() {
    used = true;

    StringBuffer jsonData = new StringBuffer("{");
    for (WriteOperation writeOperation : new WriteOperation[] {
        WriteOperation.CREATE, WriteOperation.UPDATE}) {
      String jsonDataForOperation = getJsonForOperation(writeOperation);
      if (jsonDataForOperation.equals("")) {
        continue;
      }
      if (jsonData.length() > 1) {
        jsonData.append(",");
      }
      jsonData.append(jsonDataForOperation);
    }
    jsonData.append("}");
    return jsonData.toString();
  }

  /**
   * returns true if a new change record has been added.
   */
  private <V> boolean addNewChangeRecord(EntityProxyId recordKey,
      ProxyImpl recordImpl, Property<V> property, V value) {
    ProxyJsoImpl rawMasterRecord = master.records.get(recordKey);
    ProxyJsoImpl changeRecord = newChangeRecord(recordImpl);
    if (isRealChange(property, value, rawMasterRecord)) {
      changeRecord.set(property, value);
      updates.put(recordKey, changeRecord);
      operations.put(recordKey, WriteOperation.UPDATE);
      return true;
    }
    return false;
  }

  private void checkArgumentsAndState(EntityProxy record, String methodName) {
    if (used) {
      throw new IllegalStateException(methodName
          + " can only be called on an un-used DeltaValueStore");
    }
    if (!(record instanceof ProxyImpl)) {
      throw new IllegalArgumentException(record + " + must be an instance of "
          + ProxyImpl.class);
    }
  }

  private String getJsonForOperation(WriteOperation writeOperation) {
    assert (writeOperation == WriteOperation.CREATE || writeOperation == WriteOperation.UPDATE);
    Map<EntityProxyId, ProxyJsoImpl> recordsMap = getRecordsMap(writeOperation);
    if (recordsMap.size() == 0) {
      return "";
    }
    StringBuffer requestData = new StringBuffer("\"" + writeOperation.name()
        + "\":[");
    boolean first = true;
    for (Map.Entry<EntityProxyId, ProxyJsoImpl> entry : recordsMap.entrySet()) {
      ProxyJsoImpl impl = entry.getValue();
      if (first) {
        first = false;
      } else {
        requestData.append(",");
      }
      requestData.append("{\""
          + entry.getValue().getSchema().getToken() + "\":");
      requestData.append(impl.toJson());
      requestData.append("}");
    }
    requestData.append("]");
    return requestData.toString();
  }

  private EntityProxyIdImpl getPersistedProxyId(Long datastoreId,
      ProxySchema<?> schema) {
    return new EntityProxyIdImpl(datastoreId, schema,
        RequestFactoryJsonImpl.NOT_FUTURE,
        requestFactory.datastoreToFutureMap.get(datastoreId, schema));
  }

  private Map<EntityProxyId, ProxyJsoImpl> getRecordsMap(
      WriteOperation writeOperation) {
    switch (writeOperation) {
      case CREATE:
        return creates;
      case UPDATE:
        return updates;
      default:
        throw new IllegalStateException("unknow writeOperation "
            + writeOperation.name());
    }
  }

  private Map<Long, Map<String, String>> getViolationsMap(
      JsArray<ReturnRecord> records) {
    Map<Long, Map<String, String>> violationsMap = new HashMap<Long, Map<String, String>>();
    int length = records.length();
    for (int i = 0; i < length; i++) {
      ReturnRecord record = records.get(i);
      HashMap<String, String> violations = null;
      if (record.hasViolations()) {
        violations = new HashMap<String, String>();
        record.fillViolations(violations);
      } else {
        violations = NULL_VIOLATIONS;
      }
      violationsMap.put(record.getId(), violations);
    }
    return violationsMap;
  }

  private <V> boolean isRealChange(Property<V> property, V value,
      ProxyJsoImpl rawMasterRecord) {
    ProxyJsoImpl masterRecord = null;

    if (rawMasterRecord == null) {
      return true;
    }

    masterRecord = rawMasterRecord.cast();

    if (!masterRecord.isDefined(property.getName())) {
      return true;
    }

    V masterValue = masterRecord.get(property);

    if (masterValue == value) {
      return false;
    }

    if ((masterValue != null)) {
      return !masterValue.equals(value);
    }

    return true;
  }

  private SyncResultImpl makeSyncResult(ProxyJsoImpl jso,
      Map<String, String> violations, Object futureId) {
    return new SyncResultImpl(jso.getSchema().create(jso), violations, futureId);
  }

  private ProxyJsoImpl newChangeRecord(ProxyImpl fromRecord) {
    return ProxyJsoImpl.emptyCopy(fromRecord.asJso());
  }

  private void processToRemove(Set<EntityProxyId> toRemove,
      WriteOperation writeOperation) {
    for (EntityProxyId recordKey : toRemove) {
      operations.remove(recordKey);
      if (writeOperation == WriteOperation.CREATE) {
        creates.remove(recordKey);
      } else if (writeOperation == WriteOperation.UPDATE) {
        updates.remove(recordKey);
      }
    }
  }
}
