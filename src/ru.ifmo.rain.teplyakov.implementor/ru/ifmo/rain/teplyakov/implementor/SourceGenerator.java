package ru.ifmo.rain.teplyakov.implementor;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation class which generate source code
 *
 * @author Teplyakov Valery
 */
public class SourceGenerator {

    private final static String SPACE = " ";
    private final static String EOLN = System.lineSeparator();
    private final static String SEMICOLON = ";";
    private final static String OPEN_BODY = "{";
    private final static String CLOSE_BODY = "}";
    private final static String COMMA = ", ";

    private final Class<?> clazz;

    private final Map<Class<?>, Map<String, String>> actualTypes;

    private StringBuilder source;

    /**
     * Creates new instance of {@link SourceGenerator}.
     * Initializes the required fields, including calculating {@link #actualTypes}
     * using {@link #getActualClassTypes(Class)}.
     *
     * @param clazz class or interface which need to implement.
     */
    SourceGenerator(Class<?> clazz) throws ImplerException {
        this.clazz = clazz;
        this.source = new StringBuilder();
        this.actualTypes = getActualClassTypes(clazz);
    }

    /**
     * Generate source code for implementation of {@link #clazz}. The result is written in {@link #source}.
     * Not recalculated when re-accessed.
     *
     * @return source code in {@link String} type.
     * @throws ImplerException if generate class head (using {@link #addClassHead()}),
     *                         constructors (using {@link #addConstructors()}) or
     *                         addAbstractMethods (using {@link #addAbstractMethods()}})
     *                         failed.
     */
    public String generate() throws ImplerException {
        if (source.length() > 0) {
            return source.toString();
        }

        addPackage();
        addClassHead();
        addConstructors();
        addAbstractMethods();
        source.append(CLOSE_BODY).append(EOLN);
        return source.toString();
    }

    private static class MethodWrapper {

        /**
         * Create a wrapper for passed instance of {@link Method}.
         *
         * @param other instance if {@link Method}.
         */
        MethodWrapper(Method other) {
            method = other;
        }

        @Override
        public int hashCode() {
            return ((Arrays.hashCode(method.getParameterTypes()) * 31
                    + method.getName().hashCode())) * 31
                    + method.getReturnType().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj instanceof MethodWrapper) {
                return method.getName().equals(((MethodWrapper) obj).method.getName())
                        && method.getReturnType().equals(((MethodWrapper) obj).method.getReturnType())
                        && Arrays.equals(method.getParameterTypes(), ((MethodWrapper) obj).method.getParameterTypes());

            } else {
                return false;
            }
        }

        /**
         * Getter for {@link #method}.
         *
         * @return wrapped instance of {@link Method}.
         */
        Method getMethod() {
            return method;
        }

        private final Method method;
    }

    /**
     * Adds "Impl" suffix to simple name of given class.
     *
     * @param token class to get name.
     * @return {@link String} with receive class name.
     */
    public static String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    private void addPackage() {
        if (!clazz.getPackage().getName().equals("")) {
            source.append("package ").append(clazz.getPackageName()).append(SEMICOLON).append(EOLN);
        }
        source.append(EOLN);
    }

    private static String getTypeParametersDiamond(TypeVariable<?>[] types,
                                                   Map<String, String> actualClassTypes,
                                                   boolean isTypeDeclaration) throws ImplerException {
        return getTypeParametersDiamond(types, actualClassTypes, getSet(types), isTypeDeclaration);
    }

    private static String getTypeParametersBounds(TypeVariable<?> type,
                                                  Map<String, String> actualClassTypes,
                                                  Set<TypeVariable<?>> paramTypes) throws ImplerException {
        Type[] bounds = type.getBounds();
        if (bounds.length > 0 && !checkForObject(bounds[0])) {
            StringJoiner sj = new StringJoiner(SPACE + "&" + SPACE, SPACE + "extends" + SPACE, "");
            for (Type boundType : bounds) {
                sj.add(getActualType(boundType, actualClassTypes, paramTypes));
            }
            return sj.toString();
        }

        return "";
    }

    private static String getTypeParametersDiamond(Type[] types,
                                                   Map<String, String> actualClassTypes,
                                                   Set<TypeVariable<?>> paramTypes,
                                                   boolean isTypeDeclaration) throws ImplerException {
        if (types.length > 0) {
            StringJoiner sj = new StringJoiner(COMMA, "<", ">");
            for (Type type : types) {
                sj.add(getActualType(type, actualClassTypes, paramTypes) +
                        (isTypeDeclaration
                                ? getTypeParametersBounds((TypeVariable<?>) type, actualClassTypes, paramTypes)
                                : ""));
            }

            return sj.toString();
        }
        return "";
    }

    private void addClassHead() throws ImplerException {
        source.append("public class ").append(getClassName(clazz));
        source.append(getTypeParametersDiamond(clazz.getTypeParameters(), actualTypes.get(clazz), true));

        source.append(SPACE).append(clazz.isInterface() ? "implements" : "extends").append(SPACE)
                .append(clazz.getSimpleName());

        source.append(getTypeParametersDiamond(clazz.getTypeParameters(), actualTypes.get(clazz), false));
        source.append(SPACE).append(OPEN_BODY).append(EOLN);
    }

    private static String getTabs(int cnt) {
        return SPACE.repeat(4 * cnt);
    }

    private static boolean checkForObject(Type type) {
        return type instanceof Class && type.equals(Object.class);
    }

    private static String getActualType(Type type,
                                        Map<String, String> actualClassTypes,
                                        Set<TypeVariable<?>> paramTypes) throws ImplerException {
        return getActualTypeHelper(type, actualClassTypes, paramTypes).replace('$', '.');
    }

    private static String getActualTypeHelper(Type type,
                                              Map<String, String> actualClassTypes,
                                              Set<TypeVariable<?>> paramTypes) throws ImplerException {

        String typeName = type.getTypeName();
        if (type instanceof Class) {
            return typeName;
        }

        if (type instanceof TypeVariable) {
            String result = paramTypes.contains(type) ? typeName : actualClassTypes.get(typeName);
            assert result != null : typeName + " " + actualClassTypes;
            return Objects.requireNonNull(result);
        }

        if (type instanceof GenericArrayType) {
            return getActualType(((GenericArrayType) type).getGenericComponentType(), actualClassTypes, paramTypes)
                    + "[]";
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return parameterizedType.getRawType().getTypeName() +
                    getTypeParametersDiamond(parameterizedType.getActualTypeArguments(), actualClassTypes, paramTypes, false);
        }

        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;

            Type[] lowerBounds = wildcardType.getLowerBounds();
            Type[] upperBounds = wildcardType.getUpperBounds();

            if (lowerBounds.length > 1 || upperBounds.length > 1
                    || (lowerBounds.length == 1 && !checkForObject(upperBounds[0]))) {
                throw new ImplerException("Can't generate type with more then one bound: " + wildcardType.getTypeName());
            }

            if (lowerBounds.length > 0) {
                return "?" + SPACE + "super" + SPACE + getActualType(lowerBounds[0], actualClassTypes, paramTypes);
            }

            if (!checkForObject(upperBounds[0])) {
                return "?" + SPACE + "extends" + SPACE + getActualType(upperBounds[0], actualClassTypes, paramTypes);
            }

            return "?";
        }

        throw new ImplerException("Can't generate class with type " + type.getClass());
    }

    private static <T> Set<T> getSet(T[] arg) {
        return new HashSet<>(Arrays.asList(arg));
    }

    private String getReturnValueAndName(Executable exec) throws ImplerException {

        StringBuilder res = new StringBuilder();

        if (exec instanceof Method) {
            String returnType = getActualType(
                    ((Method) exec).getGenericReturnType(),
                    actualTypes.get(exec.getDeclaringClass()),
                    getSet(exec.getTypeParameters())
            );

            res.append(returnType).append(SPACE).append(exec.getName());
            return res.toString();
        } else {
            res.append(getClassName(exec.getDeclaringClass()));
            return res.toString();
        }
    }

    private static String getParam(Parameter param, boolean needTypes,
                                   Map<String, String> actualClassTypes,
                                   TypeVariable<?>[] paramTypes) throws ImplerException {
        return (needTypes
                ? getActualType(param.getParameterizedType(), actualClassTypes, getSet(paramTypes)) + SPACE
                : "")
                + param.getName();
    }

    private String getParams(Executable exec, boolean isNeedTypes) throws ImplerException {
        StringJoiner res = new StringJoiner(COMMA, "(", ")");
        for (Parameter param : exec.getParameters()) {
            res.add(getParam(param, isNeedTypes, actualTypes.get(exec.getDeclaringClass()), exec.getTypeParameters()));
        }
        return res.toString();
    }

    private static String getDefaultValue(Class<?> token) {
        if (token.equals(boolean.class)) {
            return " false";
        } else if (token.equals(void.class)) {
            return "";
        } else if (token.isPrimitive()) {
            return " 0";
        }
        return " null";
    }

    private String getBody(Executable exec) throws ImplerException {
        if (exec instanceof Method) {
            return "return" + getDefaultValue(((Method) exec).getReturnType());
        } else {
            return "super" + getParams(exec, false);
        }
    }

    private String getException(Executable exec) throws ImplerException {

        if (exec instanceof Constructor) {
            Type[] exceptionTypes = exec.getGenericExceptionTypes();
            if (exceptionTypes.length > 0) {
                StringJoiner joiner = new StringJoiner(COMMA, SPACE + "throws" + SPACE, "");
                for (Type exceptionType : exceptionTypes) {
                    joiner.add(exceptionType.getTypeName());
                }
                return joiner.toString();
            }
        }
        return "";
    }

    private static String getOverride(Executable exec) {
        if (exec instanceof Method) {
            return getTabs(1) + "@Override" + EOLN;
        }

        return "";
    }

    private void addExec(Executable exec) throws ImplerException {
        final int mods = exec.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.TRANSIENT;
        source.append(EOLN)
                .append(getOverride(exec))
                .append(getTabs(1))
                .append(Modifier.toString(mods))
                .append(mods > 0 ? SPACE : "")
                .append(getTypeParametersDiamond(exec.getTypeParameters(),
                        actualTypes.get(exec.getDeclaringClass()),
                        true))
                .append(getReturnValueAndName(exec))
                .append(getParams(exec, true))
                .append(getException(exec))
                .append(SPACE)
                .append(OPEN_BODY)
                .append(EOLN)
                .append(getTabs(2))
                .append(getBody(exec))
                .append(SEMICOLON)
                .append(EOLN)
                .append(getTabs(1))
                .append(CLOSE_BODY)
                .append(EOLN);
    }

    private static void getAbstractMethods(Method[] methods, Set<MethodWrapper> methodsStorage) {
        Arrays.stream(methods)
                .map(MethodWrapper::new)
                .collect(Collectors.toCollection(() -> methodsStorage));
    }

    ///NEEEDFIX
    private void addAbstractMethods() throws ImplerException {
        Set<MethodWrapper> methods = new HashSet<>();

        getAbstractMethods(clazz.getMethods(), methods);
        Class<?> cur = clazz;
        while (cur != null) {
            getAbstractMethods(cur.getDeclaredMethods(), methods);
            cur = cur.getSuperclass();
        }

        methods = methods.stream()
                .filter(method -> Modifier.isAbstract(method.getMethod().getModifiers()))
                .collect(Collectors.toSet());

        for (MethodWrapper wrapper : methods) {
            addExec(wrapper.getMethod());
        }
    }

    private void addConstructors() throws ImplerException {
        if (clazz.isInterface()) {
            return;
        }

        Constructor<?>[] constructors = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .toArray(Constructor<?>[]::new);

        if (constructors.length == 0) {
            throw new ImplerException("No non-private constructors in class");
        }

        for (Constructor<?> constructor : constructors) {
            addExec(constructor);
        }
    }

    private static void putTypes(Class<?> supClass, Type cur,
                                 Map<Class<?>, Map<String, String>> actualTypes,
                                 Queue<Class<?>> queue) throws ImplerException {

        if (cur instanceof ParameterizedType) {
            Class<?> curClass = (Class) ((ParameterizedType) cur).getRawType();
            Map<String, String> tmp = new HashMap<>();

            ParameterizedType parameterizedType = (ParameterizedType) cur;
            Type[] typesFrom = curClass.getTypeParameters();
            Type[] typesTo = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < typesFrom.length; ++i) {
                System.out.println(actualTypes.get(supClass) + " " + typesTo[i].getTypeName());
                tmp.put(typesFrom[i].getTypeName(), Objects.requireNonNull(getActualType(typesTo[i], actualTypes.get(supClass), Collections.emptySet())));
            }

            System.out.println(supClass + " " + cur + " " + tmp);
            actualTypes.put(curClass, tmp);
            queue.add(curClass);
        }
    }

    private static Map<Class<?>, Map<String, String>> getActualClassTypes(Class<?> token) throws ImplerException {
        Map<Class<?>, Map<String, String>> actualTypes = new HashMap<>();
        Map<String, String> tmp = new HashMap<>();
        for (Type type : token.getTypeParameters()) {
            tmp.put(type.getTypeName(), type.getTypeName());
        }
        actualTypes.put(token, tmp);

        Queue<Class<?>> queue = new ArrayDeque<>();
        queue.add(token);
        while (!queue.isEmpty()) {
            Class<?> cur = queue.poll();
            Type sup = cur.getGenericSuperclass();

            if (sup != null) {
                putTypes(cur, sup, actualTypes, queue);
            }

            for (Type in : cur.getGenericInterfaces()) {
                putTypes(cur, in, actualTypes, queue);
            }
        }

        return actualTypes;
    }
}
