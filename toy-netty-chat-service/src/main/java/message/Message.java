package message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class Message implements Serializable {
    @Getter
    @AllArgsConstructor
    public enum MessageType{
        LoginRequestMessage(0, LoginRequestMessage.class),
        LoginResponseMessage(1, LoginResponseMessage.class),
        ChatRequestMessage(2, ChatRequestMessage.class),
        ChatResponseMessage(3, ChatResponseMessage.class),
        GroupCreateRequestMessage(4, GroupCreateRequestMessage.class),
        GroupCreateResponseMessage(5, GroupCreateResponseMessage.class),
        GroupJoinRequestMessage(6, GroupJoinRequestMessage.class),
        GroupJoinResponseMessage(7, GroupJoinResponseMessage.class),
        GroupQuitRequestMessage(8, GroupQuitRequestMessage.class),
        GroupQuitResponseMessage(9, GroupQuitResponseMessage.class),
        GroupChatRequestMessage(10, GroupChatRequestMessage.class),
        GroupChatResponseMessage(11, GroupChatResponseMessage.class),
        GroupMembersRequestMessage(12, GroupMembersRequestMessage.class),
        GroupMembersResponseMessage(13, GroupMembersResponseMessage.class),
        QuitRequestMessage(14, QuitRequestMessage.class), // 此字段不参与发送
        PingRequestMessage(15, PingRequestMessage.class), // 客户端发送，服务端无需回复
        RpcRequestMessage(100, RpcRequestMessage.class),
        RpcResponseMessage(101, RpcResponseMessage.class);

        private static final Map<Integer, MessageType> CODE_MAP =
                Arrays.stream(values())
                        .collect(Collectors.toMap(MessageType::getCode, type -> type));

        // 从哈希表里查，效率较高
        public static MessageType fromCode(int code) {
            MessageType type = CODE_MAP.get(code);
            if (type == null) {
                throw new IllegalArgumentException("未知的消息类型 code: " + code);
            }
            return type;
        }

        private final int code;
        private final Class<?> messageClass;
    }
    public abstract MessageType getMessageType();

    protected int sequenceId;
}
