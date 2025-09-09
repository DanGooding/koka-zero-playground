package uk.danielgooding.kokaplayground.run;

import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.danielgooding.kokaplayground.common.websocket.WebsocketServerSessionScope;

@Configuration
public class RunnerServiceConfig {

    @Bean
    public CustomScopeConfigurer customScopeConfigurer(WebsocketServerSessionScope websocketServerSessionScope) {
        CustomScopeConfigurer configurer = new CustomScopeConfigurer();
        configurer.addScope(WebsocketServerSessionScope.REFERENCE, websocketServerSessionScope);

        return configurer;
    }
}
