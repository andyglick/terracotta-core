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
package com.tc.net.protocol.tcm;

import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionHealthChecker;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandler;
import com.tc.net.protocol.transport.TransportHandshakeMessageFactory;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactory;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactoryImpl;

import java.util.List;

public class MessageTransportFactoryImpl implements MessageTransportFactory {
  private final TransportHandshakeMessageFactory transportMessageFactory;
  private final ConnectionHealthChecker          connectionHealthChecker;
  private final TCConnectionManager              connectionMgr;
  private final int                              maxReconnectTries;
  private final int                              timeout;
  private final int                              callbackport;
  private final boolean                          followRedirects;
  private final TransportHandshakeErrorHandler   defaultHandshakeErrorHandler;
  private final ReconnectionRejectedHandler      reconnectionRejectedHandler;
  private final TCSecurityManager                securityManager;

  public MessageTransportFactoryImpl(TransportHandshakeMessageFactory transportMessageFactory,
                                     ConnectionHealthChecker connectionHealthChecker,
                                     TCConnectionManager connectionManager,
                                     int maxReconnectTries,
                                     int timeout, int callbackPort, boolean follow,
                                     TransportHandshakeErrorHandler defaultHandshakeErrorHandler,
                                     ReconnectionRejectedHandler reconnectionRejectedBehaviour,
                                     TCSecurityManager securityManager) {
    this.transportMessageFactory = transportMessageFactory;
    this.connectionHealthChecker = connectionHealthChecker;
    this.connectionMgr = connectionManager;
    this.maxReconnectTries = maxReconnectTries;
    this.timeout = timeout;
    this.callbackport = callbackPort;
    this.followRedirects = follow;
    this.defaultHandshakeErrorHandler = defaultHandshakeErrorHandler;
    this.reconnectionRejectedHandler = reconnectionRejectedBehaviour;
    this.securityManager = securityManager;
  }
  
  @Override
  public ClientConnectionEstablisher createClientConnectionEstablisher() {
    ClientConnectionEstablisher clientConnectionEstablisher = new ClientConnectionEstablisher(maxReconnectTries,
                                                                                              reconnectionRejectedHandler);
    return clientConnectionEstablisher;
  }

  @Override
  public MessageTransport createNewTransport() {
    ClientMessageTransport cmt = createClientMessageTransport(
                                                              defaultHandshakeErrorHandler, transportMessageFactory,
                                                              new WireProtocolAdaptorFactoryImpl(), callbackport, this.followRedirects);
    cmt.addTransportListener(connectionHealthChecker);
    return cmt;
  }

  protected ClientMessageTransport createClientMessageTransport(TransportHandshakeErrorHandler handshakeErrorHandler,
                                                                TransportHandshakeMessageFactory messageFactory,
                                                                WireProtocolAdaptorFactory wireProtocolAdaptorFactory,
                                                                int callbackPortNum, boolean followRedirects) {
    return new ClientMessageTransport(this.connectionMgr, handshakeErrorHandler, transportMessageFactory,
                                      wireProtocolAdaptorFactory, callbackPortNum, this.timeout, followRedirects, reconnectionRejectedHandler, securityManager);
  }

  @Override
  public MessageTransport createNewTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List<MessageTransportListener> transportListeners) {
    throw new AssertionError();
  }

  @Override
  public MessageTransport createNewTransport(ConnectionID connectionId, TCConnection connection,
                                             TransportHandshakeErrorHandler handler,
                                             TransportHandshakeMessageFactory handshakeMessageFactory,
                                             List<MessageTransportListener> transportListeners) {
    throw new AssertionError();
  }

}
