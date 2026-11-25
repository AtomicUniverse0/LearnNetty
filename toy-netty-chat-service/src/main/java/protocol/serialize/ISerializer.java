package protocol.serialize;

public interface ISerializer {
    <T> byte[] serialize(T data) throws Exception;

    <T> T unserialize(byte[] bytebuffer, Class<T> targetType) throws Exception;
}
