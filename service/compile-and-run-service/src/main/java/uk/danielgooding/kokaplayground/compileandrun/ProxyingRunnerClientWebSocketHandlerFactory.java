package uk.danielgooding.kokaplayground.compileandrun;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.stereotype.Component;

@Component
public class ProxyingRunnerClientWebSocketHandlerFactory implements FactoryBean<ProxyingRunnerClientWebSocketHandler> {
    private ProxyingRunnerClientState downstreamSessionAndState;

    public void setDownstreamSessionAndState(
            ProxyingRunnerClientState downstreamSessionAndState) {
        this.downstreamSessionAndState = downstreamSessionAndState;
    }

    @Override
    public ProxyingRunnerClientWebSocketHandler getObject() throws FactoryBeanNotInitializedException {
        if (downstreamSessionAndState == null) {
            throw new FactoryBeanNotInitializedException("downstreamSessionAndState not set");
        }
        return new ProxyingRunnerClientWebSocketHandler(downstreamSessionAndState);
    }

    @Override
    public Class<?> getObjectType() {
        return ProxyingRunnerClientWebSocketHandler.class;
    }
}

