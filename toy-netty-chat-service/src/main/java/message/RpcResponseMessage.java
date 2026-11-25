package message;

import lombok.Data;

@Data
public class RpcResponseMessage extends AbstractResponseMessage{
    @Override
    public MessageType getMessageType() {
        return MessageType.RpcResponseMessage;
    }

    private Object returnValue;

    // 神了， Exception 也能作为返回值。。。
    private Exception exceptionValue;
}
