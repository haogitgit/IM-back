package com.sdust.im.socketio;
/*
 * @author luhao
 * @date 2018/5/17 13:38
 *
 */

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.sdust.im.domin.dao.ClientInfo;
import com.sdust.im.domin.dto.MessageInfo;
import com.sdust.im.mapper.SocketMapper;
import com.sdust.im.service.ChatService;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageEventHandler {

    private final SocketIOServer server;

    @Autowired
    private SocketMapper socketMapper;

    @Autowired
    private ChatService chatService;

    @Autowired
    public MessageEventHandler(SocketIOServer server)
    {
        this.server = server;
    }
    //添加connect事件，当客户端发起连接时调用，将clientid与sessionid存入数据库
    //方便后面发送消息时查找到对应的目标client,
    @OnConnect
    public void onConnect(SocketIOClient client)
    {
        //修改数据库中的状态
        String clientId = client.getHandshakeData().getSingleUrlParam("clientid");
        ClientInfo clientInfo = new ClientInfo();
        Date nowTime = new Date(System.currentTimeMillis());
        clientInfo.setClientid(clientId);
        clientInfo.setConnected((short)1);
        clientInfo.setMostsignbits(client.getSessionId().getMostSignificantBits());
        clientInfo.setLeastsignbits(client.getSessionId().getLeastSignificantBits());
        clientInfo.setLastconnecteddate(nowTime);
        try {
            socketMapper.saveClientInfo(clientInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //给自己发送好友的在线状态
        Map<String, Boolean> ownContactOnlineState = chatService.getOwnContactOnlineState(clientId);
        client.sendEvent("onlineState", ownContactOnlineState);
        //给在线好友发送自己的在线状态
        Map<String, Map<String, Boolean>> contactContactOnlineState = chatService.getContactContactOnlineState(clientId);
        for (Entry<String, Map<String, Boolean>> entry : contactContactOnlineState.entrySet()) {
            String key = entry.getKey();
            ClientInfo contactClientInfo = socketMapper.findClientInfoByClientid(key);
            if (contactClientInfo != null )
            {
                UUID uuid = new UUID(contactClientInfo.getMostsignbits(), contactClientInfo.getLeastsignbits());
                server.getClient(uuid).sendEvent("onlineState", entry.getValue());
            }
        }
    }

    //添加@OnDisconnect事件，客户端断开连接时调用，刷新客户端信息
    @OnDisconnect
    public void onDisconnect(SocketIOClient client)
    {
        String clientId = client.getHandshakeData().getSingleUrlParam("clientid");
        ClientInfo clientInfo = socketMapper.findClientInfoByClientid(clientId);
        clientInfo.setConnected((short)0);
        clientInfo.setMostsignbits(null);
        clientInfo.setLeastsignbits(null);
        try {
            socketMapper.saveClientInfo(clientInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //给好友发送自己的在线状态
        Map<String, Map<String, Boolean>> contactContactOnlineState = chatService.getContactContactOnlineState(clientId);
        for (Entry<String, Map<String, Boolean>> entry : contactContactOnlineState.entrySet()) {
            String key = entry.getKey();
            Map<String, Boolean> value = entry.getValue();
            ClientInfo contactClientInfo = socketMapper.findClientInfoByClientid(key);
            if (contactClientInfo != null )
            {
                UUID uuid = new UUID(contactClientInfo.getMostsignbits(), contactClientInfo.getLeastsignbits());
                server.getClient(uuid).sendEvent("onlineState", value);
            }
        }
    }

    //消息接收入口，当接收到消息后，查找发送目标客户端，并且向该客户端发送消息，且给自己发送消息
    @OnEvent(value = "messageevent")
    public void onEvent(SocketIOClient client, AckRequest request, MessageInfo data)
    {
        String targetClientId = data.getTargetClientId();
        ClientInfo clientInfo =  socketMapper.findClientInfoByClientid(targetClientId);
        if (clientInfo != null && clientInfo.getConnected() != 0)
        {
            UUID uuid = new UUID(clientInfo.getMostsignbits(), clientInfo.getLeastsignbits());
            System.out.println(uuid.toString());
            MessageInfo sendData = new MessageInfo();
            sendData.setSourceClientId(data.getSourceClientId());
            sendData.setTargetClientId(data.getTargetClientId());
            sendData.setMsgType("chat");
            sendData.setMsgContent(data.getMsgContent());
            sendData.setDate(data.getDate());
            client.sendEvent("messageevent", sendData);
            server.getClient(uuid).sendEvent("messageevent", sendData);
        }
//        client.sendEvent("messageevent", data);
    }
}
