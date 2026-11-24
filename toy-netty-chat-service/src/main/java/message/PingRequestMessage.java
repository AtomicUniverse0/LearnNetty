package message;

public class PingRequestMessage extends Message{
    @Override
    public MessageType getMessageType() {
        return MessageType.PingRequestMessage;
    }
}