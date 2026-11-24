package message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
public abstract class Message implements Serializable {
    @Getter
    @AllArgsConstructor
    public enum MessageType{
        LoginRequestMessage(0),
        LoginResponseMessage(1),
        ChatRequestMessage(2),
        ChatResponseMessage(3),
        GroupCreateRequestMessage(4),
        GroupCreateResponseMessage(5),
        GroupJoinRequestMessage(6),
        GroupJoinResponseMessage(7),
        GroupQuitRequestMessage(8),
        GroupQuitResponseMessage(9),
        GroupChatRequestMessage(10),
        GroupChatResponseMessage(11),
        GroupMembersRequestMessage(12),
        GroupMembersResponseMessage(13),
        QuitRequestMessage(14),
        PingRequestMessage(15); // 客户端发送，服务端无需返回

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
    }

    public static Class<?> getMessageClass( MessageType messageType){
        return messageClasses.get(messageType.getCode());
    }

    public abstract MessageType getMessageType();

    private int sequenceId;

    private static Map<Integer, Class<?>> messageClasses = new ConcurrentHashMap<>();
}
