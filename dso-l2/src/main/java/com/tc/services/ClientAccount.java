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

package com.tc.services;

import com.tc.entity.ServerEntityMessage;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;

import java.util.HashMap;
import java.util.Map;

public class ClientAccount {
  private final MessageChannel channel;
  private final Map<Long, ResponseWaiter> waitingResponse = new HashMap<>();
  private volatile boolean open = true;
  private long responseId = 0;

  ClientAccount(MessageChannel channel) {
    this.channel = channel;
  }

  synchronized ResponseWaiter send(ClientInstanceID clientInstance, byte[] payload) {
    ResponseWaiter responseWaiter = new ResponseWaiter();
    if (!open) {
      responseWaiter.done();
    } else {
      waitingResponse.put(responseId, responseWaiter);
      ServerEntityMessage message = (ServerEntityMessage) channel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE);
      message.setMessage(clientInstance, payload, responseId++);
      if (!message.send()) {
        if (waitingResponse.remove(responseId, responseWaiter)) {
          responseWaiter.done();
        }
      }
    }
    return responseWaiter;
  }

  synchronized void sendNoResponse(ClientInstanceID clientInstance, byte[] payload) {
    if (open) {
      ServerEntityMessage message = (ServerEntityMessage) channel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE);
      message.setMessage(clientInstance, payload);
      if (!message.send()) {
//  message not delivered.  This call is only best efforts so ignore.        
      }
    }
  }
  /** 
   * going to initiate the close here.  also want to shutdown all the waiters because 
   * the mapping is going to be removed from above
   */

  synchronized void close() {
    if (channel.isOpen()) {
      // if the channel is open, this means that a consumer of the ClientCommunicator API
      // has directly requested a close on the client connection.  This slightly reorders things
      // but it shouldn't matter here.  waiting on response is not something that is really supported anyways
      // (API is deprecated)
      channel.close();
    }
    open = false;
    for (ResponseWaiter responseWaiter : waitingResponse.values()) {
      // Client closed, whether or not it received the message is not important anymore since it's gone.
      responseWaiter.done();
    }
  }

  synchronized void response(long responseId) {
    if (open) {
      ResponseWaiter responseWaiter = waitingResponse.remove(responseId);
      if (responseWaiter != null) {
        responseWaiter.done();
      }
    }
  }
}
