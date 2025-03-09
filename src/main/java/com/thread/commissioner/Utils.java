package com.thread.commissioner;

import io.openthread.commissioner.ByteArray;
/**
 * @author Kuldeep Singh
 */
public class Utils {

    public static byte[] getByteArray(ByteArray byteArray) {
        if (byteArray == null) {
            return null;
        }
        byte[] ret = new byte[byteArray.size()];
        for (int i = 0; i < byteArray.size(); ++i) {
            ret[i] = byteArray.get(i);
        }
        return ret;
    }

    public static byte[] getByteArray(String hexString) {
        if (hexString == null || (hexString.length() % 2 != 0)) {
            return null;
        }
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int firstDecimal = Character.digit(hexString.charAt(i), 16);
            int secondDecimal = Character.digit(hexString.charAt(i + 1), 16);
            if (firstDecimal == -1 || secondDecimal == -1) {
                return null;
            }
            data[i / 2] = (byte) ((firstDecimal << 4) + secondDecimal);
        }
        return data;
    }

    public static String getHexString(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }

        StringBuilder strbuilder = new StringBuilder();
        for (byte b : byteArray) {
            strbuilder.append(String.format("%02x", b));
        }
        return strbuilder.toString();
    }

    public static String getHexString(ByteArray byteArray) {
        return getHexString(getByteArray(byteArray));
    }
}
