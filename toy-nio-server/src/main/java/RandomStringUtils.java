import java.util.concurrent.ThreadLocalRandom;

public class RandomStringUtils {

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int LengthUpperBound = 100;

    // UTF-8 兼容 ASCII，而 '\n' ',' '|' 等都是ASCII 编码，所以它们的 char 值都和字节值相等。
    public static final char Splitor = '\n';

    // 生成最后有个换行符的随机字符串
    public static String randomStringWithFinalSplitor(int minLength, int maxLength) {
        if (minLength < 0 || maxLength < minLength) {
            throw new IllegalArgumentException("长度范围不合法");
        }
        if(maxLength > LengthUpperBound){
            throw new IllegalArgumentException("最高长度受限");
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int length = (int) random.nextLong(minLength, (long) maxLength); // [minLength, maxLength - 1]
        StringBuilder result = new StringBuilder(length);

        for (int index = 0; index < length; index++) {
            int charIndex = random.nextInt(CHARACTERS.length());
            result.append(CHARACTERS.charAt(charIndex));
        }
        result.append(Splitor);

        return result.toString();
    }
}