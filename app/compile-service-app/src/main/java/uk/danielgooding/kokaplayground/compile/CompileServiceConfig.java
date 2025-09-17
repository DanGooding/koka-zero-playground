package uk.danielgooding.kokaplayground.compile;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
@EnableAspectJAutoProxy
public class CompileServiceConfig {

    @Bean
    TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public JedisConnectionFactory redisConnectionFactory(
            @Value("${compile.exe-cache.hostname}") String exeCacheHostname,
            @Value("${compile.exe-cache.port}") int exeCachePort) {

        // a 'standalone' redis instance (not clustered)
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(exeCacheHostname, exeCachePort);
        return new JedisConnectionFactory(config);
    }

    @Bean("exeCacheRedisTemplate")
    RedisTemplate<ExeCacheKey, byte[]> exeCacheRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<ExeCacheKey, byte[]> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        // it's important that json fields are serialised in a stable order within keys
        Jackson2ObjectMapperBuilder objectMapperBuilder = new Jackson2ObjectMapperBuilder();
        objectMapperBuilder.featuresToEnable(MapperFeature.SORT_CREATOR_PROPERTIES_BY_DECLARATION_ORDER);
        ObjectMapper objectMapper = objectMapperBuilder.build();

        template.setKeySerializer(new Jackson2JsonRedisSerializer<>(objectMapper, ExeCacheKey.class));
        template.setValueSerializer(RedisSerializer.byteArray());

        return template;
    }
}
