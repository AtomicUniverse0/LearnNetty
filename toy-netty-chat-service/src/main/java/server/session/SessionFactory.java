package server.session;

import lombok.Getter;

public abstract class SessionFactory {

    @Getter
    private final static Session session = new SessionMemoryImpl();

}
