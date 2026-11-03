package org.smart.erp.im;

import java.util.UUID;

public final class ImValues {

    private ImValues() {
    }

    public static long id(String value) {
        long n = seq(value);
        if (n == 0)
            throw ImException.invalid("ID必须为正整数字符串");
        return n;
    }

    public static long seq(String value) {
        try {
            if (value == null || !value.matches("0|[1-9][0-9]{0,18}"))
                throw new IllegalArgumentException();
            return Long.parseLong(value);
        }
        catch (IllegalArgumentException e) {
            throw ImException.invalid("序号必须为非负十进制整数字符串");
        }
    }

    public static String uuid(String value) {
        try {
            if (value == null || !UUID.fromString(value).toString().equalsIgnoreCase(value))
                throw new IllegalArgumentException();
            return value.toLowerCase(java.util.Locale.ROOT);
        }
        catch (IllegalArgumentException e) {
            throw ImException.invalid("幂等ID/clientId必须为UUID");
        }
    }

    public static int limit(Integer value) {
        if (value == null)
            return 20;
        if (value < 1 || value > 100)
            throw ImException.invalid("limit范围1～100");
        return value;
    }

    public static String name(String value) {
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > 64)
            throw ImException.invalid("群名为1～64字符");
        return value.strip();
    }

}
