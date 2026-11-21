package message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class LoginRequestMessage extends Message{
    private String userName;
    private String password;
    private String nickname;

    public LoginRequestMessage(String userName, String password, String nickname) {
        this.userName = userName;
        this.password = password;
        this.nickname = nickname;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.LoginRequestMessage;
    }
}
