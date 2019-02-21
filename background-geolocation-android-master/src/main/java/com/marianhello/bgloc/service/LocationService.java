package com.marianhello.bgloc.service;

import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.WebSocketTrans;

import java.util.List;

public interface LocationService {
    void start();
    void stop();
    void startForeground();
    void stopForeground();
    void configure(Config config);
    void registerHeadlessTask(String jsFunction);
    void startHeadlessTask();
    void executeProviderCommand(int command, int arg);

    void setUserInfoAndOpenWS(String info);

   /* void setToken(String token);
    void setUserID(String id);
    void setServerIP(String IP);
    void setUsers(List<String> ids);
    void openWebSocket(WebSocketTrans i);
    void subscribeAllUsers();
    void unSubscribeAllUsers();*/
}
