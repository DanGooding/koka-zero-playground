package uk.danielgooding.kokaplayground.compile;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import uk.danielgooding.kokaplayground.common.KokaSourceCode;

import java.util.Optional;

@Service
public class ExeCache {

    @Resource(name = "exeCacheRedisTemplate")
    private ValueOperations<ExeCacheKey, byte[]> redisValueOperations;

    @Value("${compiler.version-hash}")
    String compilerVersionHash;

    public Optional<byte[]> getCompiledExe(KokaSourceCode code, boolean optimise) {
        ExeCacheKey key = ExeCacheKey.forSourceCode(code, optimise, compilerVersionHash);
        return Optional.ofNullable(redisValueOperations.get(key));
    }

    public void putCompiledExe(KokaSourceCode code, boolean optimise, byte[] exe) {
        ExeCacheKey key = ExeCacheKey.forSourceCode(code, optimise, compilerVersionHash);
        redisValueOperations.set(key, exe);
    }

}
