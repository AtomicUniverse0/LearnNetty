package protocol.serialize.impl;

import com.google.gson.Gson;
import protocol.serialize.ISerializer;

import java.nio.charset.StandardCharsets;

public class JSONSerializer implements ISerializer {
    private static final Gson GSON = new Gson();

    @Override
    public <T> byte[] serialize(T data) throws Exception {
        return GSON.toJson(data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T unserialize(byte[] bytebuffer, Class<T> targetType) throws Exception {
        return GSON.fromJson(new String(bytebuffer, StandardCharsets.UTF_8), targetType);
    }
}
