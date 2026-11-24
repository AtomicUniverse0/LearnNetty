package util;

/** 通用参数校验工具。 */
public final class ArgumentChecker {
    private ArgumentChecker() {
    }

    /** 校验字符串非 null 且包含非空白字符，返回原字符串。 */
    public static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value;
    }
}
