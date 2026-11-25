package config;

import lombok.Getter;
import protocol.serialize.factory.SerializerFactory;

public class Config {
    @Getter
    public static SerializerFactory.SerializeType SERIALIZE_TYPE = SerializerFactory.SerializeType.JSON;
}
