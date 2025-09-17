package uk.danielgooding.kokaplayground.compile;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.danielgooding.kokaplayground.common.KokaSourceCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ExeCacheKey {
    private static final String digestAlgorithm = "SHA-1";

    /// Hash of the compiler itself - used to ensure we invalidate all entries
    /// when a new compiler version is released.
    private final String compilerHash;

    /// Hash of the source-code - keys should not be long.
    private final byte[] sourceCodeHash;

    /// Was this compiled with optimisations enabled
    private final boolean optimised;

    public ExeCacheKey(
            @JsonProperty("sourceCodeHash") byte[] sourceCodeHash,
            @JsonProperty("optimised") boolean optimised,
            @JsonProperty("compilerHash") String compilerHash) {
        this.sourceCodeHash = sourceCodeHash;
        this.optimised = optimised;
        this.compilerHash = compilerHash;
    }

    public static ExeCacheKey forSourceCode(KokaSourceCode code, boolean optimised, String compilerHash) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(digestAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            // Ideally we should be able to assert this will work once at startup.
            // Regardless, we cannot continue if it fails.
            throw new RuntimeException("missing algorithm provider", e);
        }
        md.update(code.getCode().getBytes(StandardCharsets.UTF_8));
        byte[] hash = md.digest();
        return new ExeCacheKey(hash, optimised, compilerHash);
    }

    @JsonGetter("sourceCodeHash")
    public byte[] getSourceCodeHash() {
        return sourceCodeHash;
    }

    @JsonGetter("optimised")
    public boolean isOptimised() {
        return optimised;
    }

    @JsonGetter("compilerHash")
    public String getCompilerHash() {
        return compilerHash;
    }
}
