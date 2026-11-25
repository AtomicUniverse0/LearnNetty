package protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.AllArgsConstructor;
import message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson2.JSON;
import protocol.serialize.ISerializer;
import protocol.serialize.factory.SerializerFactory;

import java.io.*;
import java.util.List;

// 自定义的编解码器，用于实现自定义的聊天通信协议
@ChannelHandler.Sharable
@AllArgsConstructor
public class MessageCodecSharable extends MessageToMessageCodec<ByteBuf, Message> {
    public static final int MAGIC = 114514;
    public static final byte VERSION = 1;

    // TODO: 读它
    // 这行代码正是 SLF4J 生成的。稍后深入看一下它的内部实现
    private static final Logger log = LoggerFactory.getLogger(MessageCodecSharable.class);

    private SerializerFactory.SerializeType serializeType;

    /*
    * 自定义的协议内容如下:
    * magic: 4字节
    * 版本号: 1字节
    * 指令类型: 1字节
    * 序列化算法: 1字节，考虑 jdk 和 json，后续还会接着扩展
    * 空字节: 1字节，为了使得消息头长度达到16，2的倍数，方便对齐
    * 序列号: 4字节，这似乎是一个实现异步通信的关键字段
    * 消息体长度: 4字节，一条消息最多支持4GB
    * */
    @Override
    public void encode(ChannelHandlerContext ctx, Message msg, List<Object> outList) throws Exception {
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
        out.writeInt(MAGIC);
        out.writeByte(VERSION);
        out.writeByte(msg.getMessageType().getCode());
        out.writeByte(serializeType.getCode());
        out.writeByte(0xff); // 占位符
        out.writeInt(msg.getSequenceId());

        ISerializer serializer = SerializerFactory.getSerializer(serializeType);
        byte[] payloadBytes = serializer.serialize(msg);

        out.writeInt(payloadBytes.length);
        out.writeBytes(payloadBytes);

        outList.add(out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int magic = in.readInt();
        if(magic != MAGIC){
            throw new IllegalArgumentException("mgaic mismatch");
        }

        int version = in.readByte();
        Message.MessageType messageType = Message.MessageType.fromCode(in.readByte());
        SerializerFactory.SerializeType serializeType = SerializerFactory.SerializeType.fromCode(in.readByte());
        in.readByte(); // 读取占位字节
        int sequenceId = in.readInt();

        // 读取载荷
        int length = in.readInt();
        byte[] payloadBytes = new byte[length];
        in.readBytes(payloadBytes, 0, length);

        ISerializer serializer = SerializerFactory.getSerializer(serializeType);
        var message = serializer.unserialize(payloadBytes, messageType.getMessageClass());

        log.info("magic: {}, version: {}, messageType: {}, serializeType: {}, sequenceId: {}", magic, version, messageType, serializeType, sequenceId);
        log.info("length: {}, Message: {}", length, JSON.toJSONString(message));
        out.add(message);
    }

}