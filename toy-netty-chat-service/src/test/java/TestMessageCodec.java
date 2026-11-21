import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import message.LoginRequestMessage;
import org.junit.jupiter.api.Test;
import protocol.MessageCodec;

import java.util.ArrayList;
import java.util.List;

public class TestMessageCodec {

    @Test
    public void testMessageCodecEncode() throws Exception {
        // 看一下输出就发现，序列化后的结果里和java耦合了，没法兼容其余语言

        // 检验 encode 方法
        EmbeddedChannel channel = new EmbeddedChannel(
            new LengthFieldBasedFrameDecoder(1024, 12, 4, 0, 0),
            new LoggingHandler(LogLevel.INFO),
            new MessageCodec(MessageCodec.SerializeType.JDK)
        );

        LoginRequestMessage loginRequestMessage = new LoginRequestMessage("zhangsan", "114514", "yeshouxianbei");
        channel.writeOutbound(loginRequestMessage);

        // 检验 decode 方法
        List<Object> byteBufList = new ArrayList<>();
        var messageCodec = new MessageCodec(MessageCodec.SerializeType.JDK);
        messageCodec.encode(null, loginRequestMessage, byteBufList);

        assert byteBufList.size() == 1;
        ByteBuf byteBuf = (ByteBuf) byteBufList.get(0);
        channel.writeInbound(byteBuf);

    }
}
