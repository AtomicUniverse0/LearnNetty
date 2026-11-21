package server.service;
import lombok.Getter;

@Getter
public abstract class UserServiceFactory {
    private static final UserService userService = new UserServiceMemoryImpl();
}
