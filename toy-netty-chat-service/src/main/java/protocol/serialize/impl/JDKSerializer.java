package protocol.serialize.impl;

import protocol.serialize.ISerializer;

import java.io.*;

public class JDKSerializer implements ISerializer {
    @Override
    public <T> byte[] serialize(T data) throws Exception {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(data);

        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public <T> T unserialize(byte[] bytebuffer, Class<T> targetType) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytebuffer);
        ObjectInputStream ois = new ObjectInputStream(bais);

        return targetType.cast(ois.readObject());
    }
}
