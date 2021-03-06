/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.locks;

import com.tc.net.NodeID;
import com.tc.object.locks.ClientServerExchangeLockContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NotifiedWaiters {

  private final Map<NodeID, Set<ClientServerExchangeLockContext>> notifiedSets = new HashMap<>();

  @Override
  public String toString() {
    synchronized (notifiedSets) {
      return "NotifiedWaiters[" + notifiedSets + "]";
    }
  }

  public boolean isEmpty() {
    return notifiedSets.isEmpty();
  }

  public void addNotification(ClientServerExchangeLockContext context) {
    synchronized (notifiedSets) {
      getOrCreateSetFor(context.getNodeID()).add(context);
    }
  }

  public Set<ClientServerExchangeLockContext> getNotifiedFor(NodeID nodeID) {
    synchronized (notifiedSets) {
      Set<ClientServerExchangeLockContext> rv = getSetFor(nodeID);
      return (rv == null) ? Collections.emptySet() : rv;
    }
  }

  private Set<ClientServerExchangeLockContext> getSetFor(NodeID nodeID) {
    return notifiedSets.get(nodeID);
  }

  private Set<ClientServerExchangeLockContext> getOrCreateSetFor(NodeID nodeID) {
    Set<ClientServerExchangeLockContext> rv = getSetFor(nodeID);
    if (rv == null) {
      rv = new HashSet<>();
      notifiedSets.put(nodeID, rv);
    }
    return rv;
  }

}
