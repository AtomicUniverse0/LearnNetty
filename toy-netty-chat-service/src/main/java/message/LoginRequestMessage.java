package message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class LoginRequestMessage extends Message{
    private String userName;
    private String password;

    public LoginRequestMessage(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.LoginRequestMessage;
    }
}
