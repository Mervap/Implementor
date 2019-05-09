package ru.ifmo.rain.teplyakov.implementor;

import java.nio.file.Path;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public interface JarImpler extends Impler {
    /**
     * Produces {@code .jar} file implementing class or interface specified by provided {@code token}.
     * <p>
     * Generated class classes name same as classes name of the type token with {@code Impl} suffix
     * added.
     *
     * @param token type token to create implementation for.
     * @param jarFile target {@code .jar} file.
     * @throws ImplerException when implementation cannot be generated.
     */
    void implementJar(Class<?> token, Path jarFile) throws ImplerException;
}
