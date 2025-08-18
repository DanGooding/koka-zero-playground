package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.web.socket.CloseStatus;

import java.io.IOException;

// TODO: this class should be basically internal
// we want all code to use SessionAndState
public interface ITypedWebSocketSession<OutboundMessage> {

    SessionId getId();

    void sendMessage(OutboundMessage messageObject) throws Exception;

    void closeOk() throws Exception;

    void close(CloseStatus closeStatus) throws Exception;
}
