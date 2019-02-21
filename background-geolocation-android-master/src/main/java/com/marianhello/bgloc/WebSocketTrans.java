package com.marianhello.bgloc;

import ua.naiksoftware.stomp.StompClient;

public interface WebSocketTrans {

    void onWebSocketOpened(StompClient stompClient);
}
