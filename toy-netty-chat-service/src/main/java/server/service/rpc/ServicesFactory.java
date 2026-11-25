package server.service.rpc;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServicesFactory {

    static Map<Class<?>, Object> map;

    static {
        try {
            map = scanClasses();
        } catch (IOException | URISyntaxException | ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Map<Class<?>, Object> scanClasses()
            throws IOException, URISyntaxException, ReflectiveOperationException {
        // 我去，可以通过类来获得包名，类加载器，
        String packageName = ServicesFactory.class.getPackageName();
        String packagePath = packageName.replace('.', '/');
        ClassLoader classLoader = ServicesFactory.class.getClassLoader();

        Enumeration<URL> resources = classLoader.getResources(packagePath);
        Map<Class<?>, Object> services = new ConcurrentHashMap<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();

            if (!"file".equals(resource.getProtocol())) {
                throw new IOException("Unsupported service resource: " + resource);
            }
            scanPackage(Path.of(resource.toURI()), packageName, classLoader, services);
        }
        if (services.isEmpty()) {
            throw new IllegalStateException("No services found in " + packageName);
        }
        return services;
    }

    private static void scanPackage(Path directory, String packageName, ClassLoader classLoader,
                                    Map<Class<?>, Object> services)
            throws IOException, ReflectiveOperationException {

        Class<?> interfaceClass = null;
        Class<?> implementationClass = null;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                String fileName = entry.getFileName().toString();
                if (Files.isDirectory(entry)) {
                    scanPackage(entry, packageName + "." + fileName, classLoader, services);
                    continue;
                }
                if (!Files.isRegularFile(entry) || !fileName.endsWith(".class")) {
                    continue;
                }
                String className = packageName + "." + fileName.substring(0, fileName.length() - 6);
                Class<?> candidate = Class.forName(className, false, classLoader);
                if (candidate == ServicesFactory.class) {
                    continue;
                }
                if (candidate.isInterface() && candidate.getSimpleName().endsWith("Service")) {
                    if (interfaceClass != null) {
                        throw new IllegalStateException("Multiple service interfaces in " + packageName);
                    }
                    interfaceClass = candidate;
                } else if (!candidate.isInterface() && !Modifier.isAbstract(candidate.getModifiers())) {
                    if (implementationClass != null) {
                        throw new IllegalStateException("Multiple implementations in " + packageName);
                    }
                    implementationClass = candidate;
                }
            }
        }
        if (interfaceClass == null && implementationClass == null) {
            return;
        }
        if (interfaceClass == null || implementationClass == null
                || !interfaceClass.isAssignableFrom(implementationClass)) {
            throw new IllegalStateException("Invalid service interface or implementation in " + packageName);
        }
        if (services.containsKey(interfaceClass)) {
            throw new IllegalStateException("Duplicate service: " + interfaceClass.getName());
        }

        services.put(interfaceClass, implementationClass.getDeclaredConstructor().newInstance());
    }

    public static <T> T getService(Class<T> interfaceClass) {
        return interfaceClass.cast(map.get(interfaceClass));
    }
}
