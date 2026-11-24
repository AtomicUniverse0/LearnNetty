package client;

/** 用户输入的命令或参数格式不合法。 */
public class CommandParseException extends IllegalArgumentException {
    public CommandParseException(String message) {
        super(message);
    }

    public CommandParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
