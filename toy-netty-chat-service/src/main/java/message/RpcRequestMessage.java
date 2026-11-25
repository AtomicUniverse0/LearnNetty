package message;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// 好好思考，一个RPC 请求里应该携带什么东西？
@Data
public class RpcRequestMessage extends Message{
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public RpcRequestMessage(
            String interfaceName,
            String methodName,
            Class<?> returnType,
            Class<?>[] argumentTypes,
            Object[] arguments) {
        super(COUNTER.getAndIncrement());
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.returnType = returnType;
        this.argumentTypes = argumentTypes;
        this.arguments = arguments;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.RpcRequestMessage;
    }

    // 这个应该是指接口名全限定名吧？类似于 PSM?
    // 好吧，其实是类名。。。
    private String interfaceName;

    private String methodName;

    Class<?> returnType;

    Class<?>[] argumentTypes;

    Object[] arguments;
}
