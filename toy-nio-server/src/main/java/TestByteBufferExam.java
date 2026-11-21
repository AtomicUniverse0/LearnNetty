import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TestByteBufferExam {
    public static void main(String[] args) {
        // 用ByteBuffer编写程序解决粘包，半包问题。

        ByteBuffer source = ByteBuffer.allocate(50);
        // 这段String占有多少字节？
        source.put("Hello\nThis is Zhang San\nI n".getBytes());
        split(source);
        source.put("eed help!\n".getBytes());
        split(source);
    }

    private static void split(ByteBuffer source) {
        source.flip();

        for(int i=0; i < source.limit(); ++i){
            // 这个'\n' 是 byte 还是 char?
            if('\n' == source.get(i)){
                int length = i + 1 - source.position();

                ByteBuffer target = ByteBuffer.allocate(length);

                while(length > 0){
                    --length;

                    byte b = source.get();
                    target.put(b);
                }

                target.flip();
                System.out.print(StandardCharsets.UTF_8.decode(target));
            }
        }

        source.compact();
    }
}
