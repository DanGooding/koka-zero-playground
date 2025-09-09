package uk.danielgooding.kokaplayground.common.websocket;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebsocketServerSessionScope implements Scope {
    public static final String REFERENCE = "websocket-server-session";

    private final Map<String, Map<String, Object>> beansByNameByScope;

    public WebsocketServerSessionScope() {
        this.beansByNameByScope = new ConcurrentHashMap<>();
    }

    @Override
    public @NonNull Object get(@NonNull String beanName, @NonNull ObjectFactory<?> beanFactory) {
        Map<String, Object> beansByName =
                beansByNameByScope.computeIfAbsent(getConversationId(), k -> new ConcurrentHashMap<>());

        return beansByName.computeIfAbsent(beanName, ignored -> beanFactory.getObject());
    }

    @Override
    public Object remove(@NonNull String name) {
        Map<String, Object> beansByName = beansByNameByScope.get(getConversationId());
        if (beansByName == null) return null;
        return beansByName.remove(name);
    }

    @Override
    public void registerDestructionCallback(@NonNull String name, @NonNull Runnable callback) {
        WebsocketServerSessionHolder
                .getInstance()
                .getSession()
                .getOutcomeFuture()
                .whenComplete((result, exn) -> callback.run());
    }

    @Override
    public Object resolveContextualObject(@NonNull String key) {
        return REFERENCE.equals(key);
    }

    @Override
    public String getConversationId() {
        return WebsocketServerSessionHolder.getInstance().getSession().toString();
    }
}
