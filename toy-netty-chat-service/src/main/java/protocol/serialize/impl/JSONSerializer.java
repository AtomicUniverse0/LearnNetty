package protocol.serialize.impl;

import com.google.gson.*;
import protocol.serialize.ISerializer;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class JSONSerializer implements ISerializer {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Class.class, new ClassCodec())
            .create();

    @Override
    public <T> byte[] serialize(T data) throws Exception {
        return GSON.toJson(data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T unserialize(byte[] bytebuffer, Class<T> targetType) throws Exception {
        return GSON.fromJson(new String(bytebuffer, StandardCharsets.UTF_8), targetType);
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
