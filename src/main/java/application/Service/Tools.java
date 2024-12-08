package application.Service;


import application.Enum.EntryStructure;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 辅助工具类，包含一些常用辅助方法
 */
public class Tools {
    /**
     * 检查文件名（含type）
     *
     * @param isDir        是文件还是目录
     * @param nameWithType 若是文件，则为“name.type” 若是目录，则就是name
     * @return null:不合法；否则返回文件名和type的二维数组
     */
    public static byte[][] checkNameAndType(String nameWithType, boolean isDir) {
        byte[] name = new byte[EntryStructure.NAME_LENGTH.getValue()];
        byte[] type = new byte[EntryStructure.TYPE_LENGTH.getValue()];
        byte[][] bytes = new byte[2][];

        if (isDir) {
            if (nameWithType.length() > 3) { //超三字节
                return null;
            }
            if (nameWithType.contains("$") || nameWithType.contains(".") || nameWithType.contains("\\")) {
                return null; // 含有非法字符
            }
            System.arraycopy(nameWithType.getBytes(), 0, name, 0, nameWithType.getBytes().length);
        } else {
            String[] nwt = nameWithType.split("\\.");
            if (nwt.length == 1) {
                // 没有拓展名则增加nwt[1]并默认为tx
                nwt = new String[]{nwt[0], "tx"};
            }
            if (nwt[0].length() > 3 || nwt[1].length() > 2) {
                return null; // 超出限定字节
            }
            if (nwt[0].contains("$") || nwt[0].contains(".") || nwt[0].contains("\\") || nwt[0].contains("/")) {
                return null; // 含有非法字符
            }
            if (nwt[1].contains("$") || nwt[1].contains(".") || nwt[1].contains("\\") || nwt[1].contains("/")) {
                return null; // 含有非法字符
            }
            System.arraycopy(nwt[0].getBytes(), 0, name, 0, nwt[0].getBytes().length);
            System.arraycopy(nwt[1].getBytes(), 0, type, 0, nwt[1].getBytes().length);
        }

        bytes[0] = name;
        bytes[1] = type;
        return bytes;
    }

    /**
     * 检查结果
     * 操作错误码
     * 1:成功；
     * 0:只读属性无法创建文件；
     * -1:路径不存在；
     * -2:文件已存在；
     * -3:无空闲磁盘块；
     * -4：根目录已满；
     * -5:文件名不合法；
     * -6：文件已打开；
     * -7：目录非空
     * -8: 根目录无法删除
     *
     * @param result 需要判断的结果
     * @return 结果码对应的错误信息
     */
    public static String checkResult(int result) {
        switch (result) {
            case 1:
                return "1";
            case 0:
                return "ERROR: Cannot create file with read-only attribute.";
            case -1:
                return "ERROR: Path does not exist.";
            case -2:
                return "ERROR: File Or Dir already exists.";
            case -3:
                return "ERROR: No free disk blocks available.";
            case -4:
                return "ERROR: Root directory is full.";
            case -5:
                return "ERROR: Name is invalid.";
            case -6:
                return "ERROR: File already opened.";
            case -7:
                return "ERROR: Directory is not empty.";
            case -8:
                return "ERROR: Root directory cannot be deleted.";
            default:
                return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 记录错误信息
     *
     * @param errorMessage 错误信息
     */
    public static void logError(String errorMessage, String LOG_FILE_NAME) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_NAME, true))) {
            writer.write(errorMessage);
            writer.newLine(); // 写入新行，保持日志格式整洁
            writer.flush(); // 清空缓冲区，确保日志信息被写入文件
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}
