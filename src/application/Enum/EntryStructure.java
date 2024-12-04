package application.Enum;

/**
 * 目录项结构枚举
 */
public enum EntryStructure {
    NAME_POS(0), // 文件名开始位置
    NAME_END(3), // 文件名结束位置
    NAME_LENGTH(3),  // 文件名长度
    TYPE_POS(3),  // 文件类型开始位置
    TYPE_END(5), // 文件类型结束位置
    TYPE_LENGTH(2), // 文件类型长度
    ATTRIBUTE_POS(5), // 文件属性位置
    START_NUM_POS(6),  // 起始盘块号位置
    DISK_BLOCK_LENGTH_POS(7), // 文件长度位置
    ENTRY_LENGTH(8); // 条目长度

    private final int value;

    EntryStructure(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
