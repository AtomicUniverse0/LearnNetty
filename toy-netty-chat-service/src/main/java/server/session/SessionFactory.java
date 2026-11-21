package server.session;

import lombok.Getter;

@Getter
public abstract class SessionFactory {

    private final static Session session = new SessionMemoryImpl();

}
