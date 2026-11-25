package message;

import lombok.Data;

// 好好思考，一个RPC 请求里应该携带什么东西？
@Data
public class RpcRequestMessage extends Message{
    @Override
    public MessageType getMessageType() {
        return MessageType.RpcRequestMessage;
    }

    // 这个应该是指接口名全限定名吧？类似于 PSM?
    private String interfaceName;

    private String methodName;

    Class<?> returnType;

    Class<?>[] argumentTypes;

    Object[] arguments;
}
