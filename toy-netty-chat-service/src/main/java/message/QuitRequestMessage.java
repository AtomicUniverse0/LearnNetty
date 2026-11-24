package message;

/** 客户端本地退出指令，由客户端关闭连接，不发送给服务端。 */
public final class QuitRequestMessage extends Message {
    @Override
    public MessageType getMessageType() {
        return MessageType.QuitRequestMessage;
    }
}
