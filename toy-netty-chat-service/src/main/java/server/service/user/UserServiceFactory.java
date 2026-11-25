package server.service.user;
import lombok.Getter;

public abstract class UserServiceFactory {
    @Getter
    private static final UserService userService = new UserServiceMemoryImpl();
}
