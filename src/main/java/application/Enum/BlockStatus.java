package application.Enum;

/**
 * 磁盘块状态枚举
 */
public enum BlockStatus {
    FREE((byte) 0), // 空闲块
    END_OF_FILE((byte) 255), // 文件结束标志
    BAD_BLOCK((byte) 254), // 坏块标志
    EMPTY_ENTRY((byte) '$'),// 空目录项标志
    EOF((byte)'#');// 文件结束标志
    private final byte value;

    BlockStatus(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    // 判断状态是否相等
    public boolean isEqual(byte status) {
        return status == value;
    }
}
