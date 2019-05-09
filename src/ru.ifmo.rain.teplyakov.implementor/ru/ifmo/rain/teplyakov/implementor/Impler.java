package ru.ifmo.rain.teplyakov.implementor;

import java.nio.file.Path;

public interface Impler {
    /**
     * Produces code implementing class or interface specified by provided {@code token}.
     * <p>
     * Generated class classes name same as classes name of the type token with {@code Impl} suffix
     * added.
     *
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException when implementation cannot be
     * generated.
     */
    void implement(Class<?> token, Path root) throws ImplerException;
}
