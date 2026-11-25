package protocol.serialize.impl;

import com.google.gson.*;
import protocol.serialize.ISerializer;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class JSONSerializer implements ISerializer {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Class.class, new ClassCodec())
            .registerTypeHierarchyAdapter(Throwable.class, new ThrowableCodec())
            .create();

    @Override
    public <T> byte[] serialize(T data) throws Exception {
        return GSON.toJson(data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T unserialize(byte[] bytebuffer, Class<T> targetType) throws Exception {
        return GSON.fromJson(new String(bytebuffer, StandardCharsets.UTF_8), targetType);
    }

    static class ThrowableCodec implements JsonSerializer<Throwable>, JsonDeserializer<Throwable> {
        @Override
        public JsonElement serialize(Throwable src, Type typeOfSrc, JsonSerializationContext context) {
            return serializeThrowable(src, Collections.newSetFromMap(new IdentityHashMap<>()));
        }

        private JsonElement serializeThrowable(Throwable throwable, Set<Throwable> visiting) {
            if (throwable == null) {
                return JsonNull.INSTANCE;
            }
            if (!visiting.add(throwable)) {
                throw new JsonIOException("Circular throwable reference: " + throwable.getClass().getName());
            }
            try {
                JsonObject json = new JsonObject();
                json.addProperty("type", throwable.getClass().getName());
                json.addProperty("message", throwable.getMessage());
                json.add("cause", serializeThrowable(throwable.getCause(), visiting));
                JsonArray stackTrace = new JsonArray();
                for (StackTraceElement frame : throwable.getStackTrace()) {
                    JsonObject element = new JsonObject();
                    element.addProperty("classLoaderName", frame.getClassLoaderName());
                    element.addProperty("moduleName", frame.getModuleName());
                    element.addProperty("moduleVersion", frame.getModuleVersion());
                    element.addProperty("className", frame.getClassName());
                    element.addProperty("methodName", frame.getMethodName());
                    element.addProperty("fileName", frame.getFileName());
                    element.addProperty("lineNumber", frame.getLineNumber());
                    stackTrace.add(element);
                }
                json.add("stackTrace", stackTrace);
                JsonArray suppressed = new JsonArray();
                for (Throwable exception : throwable.getSuppressed()) {
                    suppressed.add(serializeThrowable(exception, visiting));
                }
                json.add("suppressed", suppressed);
                return json;
            } finally {
                visiting.remove(throwable);
            }
        }

        @Override
        public Throwable deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            try {
                Class<? extends Throwable> exceptionType = Class.forName(
                        object.get("type").getAsString(), false, JSONSerializer.class.getClassLoader())
                        .asSubclass(Throwable.class);
                if (!((Class<?>) typeOfT).isAssignableFrom(exceptionType)) {
                    throw new JsonParseException("Unexpected throwable type: " + exceptionType.getName());
                }
                Throwable cause = context.deserialize(object.get("cause"), Throwable.class);
                Throwable exception = createThrowable(exceptionType, getString(object, "message"), cause);
                JsonArray stackTrace = object.getAsJsonArray("stackTrace");
                if (stackTrace != null) {
                    StackTraceElement[] frames = new StackTraceElement[stackTrace.size()];
                    for (int index = 0; index < frames.length; index++) {
                        JsonObject frame = stackTrace.get(index).getAsJsonObject();
                        frames[index] = new StackTraceElement(
                                getString(frame, "classLoaderName"), getString(frame, "moduleName"),
                                getString(frame, "moduleVersion"), getString(frame, "className"),
                                getString(frame, "methodName"), getString(frame, "fileName"),
                                frame.get("lineNumber").getAsInt());
                    }
                    exception.setStackTrace(frames);
                }
                JsonArray suppressed = object.getAsJsonArray("suppressed");
                if (suppressed != null) {
                    for (JsonElement element : suppressed) {
                        exception.addSuppressed(context.deserialize(element, Throwable.class));
                    }
                }
                return exception;
            } catch (ReflectiveOperationException | ClassCastException | IllegalArgumentException
                     | IllegalStateException exception) {
                throw new JsonParseException("Cannot restore throwable", exception);
            }
        }

        private Throwable createThrowable(Class<? extends Throwable> type, String message, Throwable cause)
                throws ReflectiveOperationException {
            try {
                return type.getConstructor(String.class, Throwable.class).newInstance(message, cause);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                return type.getConstructor(Throwable.class, String.class).newInstance(cause, message);
            } catch (NoSuchMethodException ignored) {
            }
            Throwable exception;
            try {
                exception = type.getConstructor(String.class).newInstance(message);
            } catch (NoSuchMethodException ignored) {
                try {
                    return type.getConstructor(Throwable.class).newInstance(cause);
                } catch (NoSuchMethodException missingCauseConstructor) {
                    exception = type.getConstructor().newInstance();
                }
            }
            if (cause != null) {
                exception.initCause(cause);
            }
            return exception;
        }

        private String getString(JsonObject object, String name) {
            JsonElement value = object.get(name);
            return value == null || value.isJsonNull() ? null : value.getAsString();
        }
    }

    static class ClassCodec implements JsonSerializer<Class<?>> , JsonDeserializer<Class<?>>{
        @Override
        public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return Class.forName(json.getAsString());
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e);
            }
        }

        @Override
        public JsonElement serialize(Class src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getName());
        }
    }
}
