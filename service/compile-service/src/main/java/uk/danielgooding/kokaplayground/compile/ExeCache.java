package uk.danielgooding.kokaplayground.compile;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.KokaSourceCode;

import java.util.Optional;

@Service
public class ExeCache {
    private final ValueOperations<ExeCacheKey, byte[]> redisValueOperations;
    private final String compilerVersionHash;

    private final Counter hitCounter;
    private final Counter missCounter;

    public ExeCache(
            @Autowired @Qualifier("exeCacheRedisTemplate") RedisTemplate<ExeCacheKey, byte[]> redisTemplate,
            @Value("${compiler.version-hash}") String compilerVersionHash,
            @Autowired MeterRegistry meterRegistry
    ) {
        this.redisValueOperations = redisTemplate.opsForValue();
        this.compilerVersionHash = compilerVersionHash;


        hitCounter = makeOutcomeCounter(meterRegistry, true);
        missCounter = makeOutcomeCounter(meterRegistry, false);
    }

    public Optional<byte[]> getCompiledExe(KokaSourceCode code, CompilerArgs compilerArgs) {
        ExeCacheKey key = ExeCacheKey.forSourceCode(code, compilerArgs, compilerVersionHash);

        Optional<byte[]> result = Optional.ofNullable(redisValueOperations.get(key));

        (result.isEmpty() ? missCounter : hitCounter).increment();

        return result;
    }

    public void putCompiledExe(KokaSourceCode code, CompilerArgs compilerArgs, byte[] exe) {
        ExeCacheKey key = ExeCacheKey.forSourceCode(code, compilerArgs, compilerVersionHash);
        redisValueOperations.set(key, exe);
    }

    private Counter makeOutcomeCounter(MeterRegistry meterRegistry, boolean hit) {
        return Counter.builder("cache_result")
                .tag("cache", "exe-cache")
                .tag("outcome", hit ? "hit" : "miss")
                .register(meterRegistry);
    }

}
