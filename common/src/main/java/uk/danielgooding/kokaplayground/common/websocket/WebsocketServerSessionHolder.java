package uk.danielgooding.kokaplayground.common.websocket;

public class WebsocketServerSessionHolder {
    private final ThreadLocal<TypedWebSocketSession<?, ?>> session =
            new ThreadLocal<>();
    private static final WebsocketServerSessionHolder instance =
            new WebsocketServerSessionHolder();

    public static WebsocketServerSessionHolder getInstance() {
        return instance;
    }

    public void setSession(TypedWebSocketSession<?, ?> session) {
        this.session.set(session);
    }

    public void clearSession() {
        this.session.set(null);
    }

    public TypedWebSocketSession<?, ?> getSession() {
        return this.session.get();
    }
}
