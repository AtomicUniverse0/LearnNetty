package protocol.serialize.factory;


import lombok.AllArgsConstructor;
import lombok.Getter;
import protocol.serialize.ISerializer;
import protocol.serialize.impl.JDKSerializer;
import protocol.serialize.impl.JSONSerializer;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class SerializerFactory {
    private static final JDKSerializer jdkSerializer = new JDKSerializer();
    private static final JSONSerializer jsonSerializer = new JSONSerializer();

    public static ISerializer getSerializer(SerializeType serializeType){
        if(serializeType == SerializeType.JDK){
            return jdkSerializer;
        }else if(serializeType == SerializeType.JSON){
            return jsonSerializer;
        }
        throw new IllegalArgumentException("Unknown serialize type: " + serializeType);
    };

    @Getter
    @AllArgsConstructor
    public enum SerializeType{
        JDK(0),
        JSON(1);

        private final static Map<Integer, SerializeType> CODE_MAP =
                Arrays.stream(values())
                        .collect(Collectors.toMap(SerializeType::getCode, serializeType -> serializeType));

        public static SerializeType fromCode(int code){
            if(CODE_MAP.containsKey(code)){
                return CODE_MAP.get(code);
            }

            throw new IllegalArgumentException(String.format("invalid code %d",code));
        }

        private final int code;
    }
}
