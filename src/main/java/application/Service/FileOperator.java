package application.Service;

import application.Entity.Entry;
import application.Entity.OFTLE;
import application.Enum.BlockStatus;
import application.Enum.EntryAttribute;
import application.Enum.EntryStructure;
import application.Manager.OFTableManager;

import java.util.Arrays;

/**
 * 文件操作，提供文件的操作接口：创建文件、读文件、写文件、关闭文件、删除文件等
 */
public class FileOperator {
    private final EntryOperator entryOperator; // 目录项操作
    private final OFTableManager ofTableManager;// 打开文件表管理器

    public FileOperator(EntryOperator entryOperator, OFTableManager ofTableManager) {
        this.entryOperator = entryOperator;
        this.ofTableManager = ofTableManager;
    }

    /**
     * 创建新文件
     *
     * @param fileAbsolutePath 文件完整路径
     * @param attribute        文件属性
     * @return 1:成功； 0:只读属性无法创建； -1:路径不存在； -2:文件已存在； -3:无空闲磁盘块；-4：根目录已满；-5:文件名不合法
     */
    public int create_file(String fileAbsolutePath, byte attribute) throws Exception {
        // 文件属性如果是只读性质则不能建立
        if (EntryAttribute.READ_ONLY.isEqual(attribute)) {
            return 0;
        }

        // 解析文件路径，获取父目录盘块号，以检查父目录是否存在
        String[] pathComponents = fileAbsolutePath.split("/"); // ["", "path", "to", "your", "file.txt"]
        String parentDirName = String.join("/", Arrays.copyOfRange(pathComponents, 0, pathComponents.length - 1)); // "/path/to/your"
        int parentDirBlockIndex = this.entryOperator.findDirBlockIndex(parentDirName);
        if (parentDirBlockIndex == -1) {
            return -1;
        }

        // 检查文件名（含type）
        byte[][] nameAndType = Tools.checkNameAndType(pathComponents[pathComponents.length - 1], EntryAttribute.DIRECTORY.isEqual(attribute));
        if (nameAndType == null) {
            return -5;
        }
        // 分离文件名和类型
        byte[] fileNameBytes = new byte[EntryStructure.NAME_LENGTH.getValue()];
        byte[] fileType = new byte[EntryStructure.TYPE_LENGTH.getValue()];
        System.arraycopy(nameAndType[0], 0, fileNameBytes, 0, fileNameBytes.length);
        System.arraycopy(nameAndType[1], 0, fileType, 0, fileType.length);

        // 检查是否有重名文件
        if (this.entryOperator.findEntryInDirectory(parentDirBlockIndex, pathComponents[pathComponents.length - 1], attribute) != null) {
            return -2;
        }

        // 寻找空闲目录项，支持子目录任意长（但根目录不支持）
        int[] result = this.entryOperator.findFreeEntry(parentDirBlockIndex);
        parentDirBlockIndex = result[0];
        int freeEntryIndex = result[1];
        if (parentDirBlockIndex == 0) {
            return -4; // 根目录不支持任意长
        }
        if (parentDirBlockIndex == -1) {
            return -3; // 没有多余的磁盘块来添加目录项，所以实际上和没有多余磁盘块给文件一样，返回-3
        }

        // 分配一个新的磁盘块给文件
        int fileBlockIndex = this.entryOperator.allocateDiskBlock();
        if (fileBlockIndex == -1) {
            return -3;
        }
        // 在这个新文件中写入文件结束符
        this.entryOperator.setContentToEntry(fileBlockIndex, new byte[]{BlockStatus.EOF.getValue()});

        // 创建新的目录项并添加到父目录中
        Entry newFileEntry = new Entry(fileNameBytes, fileType, attribute, (byte) fileBlockIndex, (byte) 1);
        this.entryOperator.addEntryToDirectory(parentDirBlockIndex, freeEntryIndex, newFileEntry);

        // 最后填写已打开文件表
        //OFTLE newOftle = new OFTLE(fileAbsolutePath, attribute, fileBlockIndex, fileBlockIndex, 1, "rw");
        //ofTableManager.add(newOftle);
        return 1;
    }

    /**
     * 关闭文件
     *
     * @param fileAbsolutePath 文件名（含路径）
     * @return 关闭成功返回 1， 文件不存在返回 -1
     */
    public int closeFile(String fileAbsolutePath) throws Exception {
        // 解析文件路径，获取父目录盘块号
        String[] pathComponents = fileAbsolutePath.split("/");
        String parentDirName = String.join("/", Arrays.copyOfRange(pathComponents, 0, pathComponents.length - 1));
        int parentDirBlockIndex = this.entryOperator.findDirBlockIndex(parentDirName);
        // 寻找该登记项
        Entry fileEntry = this.entryOperator.findEntryInDirectory(
                this.entryOperator.findDirBlockIndex(parentDirName),
                pathComponents[pathComponents.length - 1],
                EntryAttribute.NORMAL_FILE.getValue()
        );
        if (fileEntry == null) return -1; // 文件不存在

        // 文件未打开
        OFTLE targetOftle = ofTableManager.find(fileEntry.getStartNum());
        if (targetOftle == null) return 1;

        // 如果是以写方式打开的，追加文件结束符并更新目录项
        if (targetOftle.getOperateFlag() == 1 || targetOftle.getOperateFlag() == 2) {
            // 追加文件结束符
            int dNum = targetOftle.getWrite().getdNum();
            int bNum = targetOftle.getWrite().getbNum();
            
        }

        // 从已打开文件表中删除对应项
        this.ofTableManager.remove(targetOftle);
        return 1;
    }


    /**
     * 删除文件
     *
     * @param fileAbsolutePath 文件名（含路径）
     * @return 删除成功返回 1， 文件不存在返回 -1， 文件已打开返回 -6
     */
    public int deleteFile(String fileAbsolutePath) throws Exception {
        // 解析文件路径，获取父目录盘块号
        String[] pathComponents = fileAbsolutePath.split("/");
        String parentDirName = String.join("/", Arrays.copyOfRange(pathComponents, 0, pathComponents.length - 1));
        int parentDirBlockIndex = this.entryOperator.findDirBlockIndex(parentDirName);
        // 寻找该登记项
        Entry fileEntry = this.entryOperator.findEntryInDirectory(
                this.entryOperator.findDirBlockIndex(parentDirName),
                pathComponents[pathComponents.length - 1],
                EntryAttribute.NORMAL_FILE.getValue()
        );
        if (fileEntry == null) return -1; // 文件不存在

        // 检查文件是否已打开
        if (ofTableManager.find(fileEntry.getStartNum()) != null) {
            return -6;
        }

        // 删除文件目录项
        int fileBlockIndex = fileEntry.getStartNum();
        String fileNameAndType = new String(fileEntry.getName()).trim() + new String(fileEntry.getType()).trim();
        this.entryOperator.dealEntry(fileNameAndType, parentDirBlockIndex, fileBlockIndex);

        return 1;
    }


    /**
     * 显示文件内容
     *
     * @param fileAbsolutePath 文件完整路径
     * @return 文件内容的字符串表示，如果文件不存在或无法读取，则返回错误信息
     */
    public String typeFile(String fileAbsolutePath) throws Exception {
        // 解析文件路径，获取父目录盘块号
        String[] pathComponents = fileAbsolutePath.split("/");
        String parentDirName = String.join("/", Arrays.copyOfRange(pathComponents, 0, pathComponents.length - 1));
        // 找到文件目录项
        Entry fileEntry = this.entryOperator.findEntryInDirectory(
                this.entryOperator.findDirBlockIndex(parentDirName),
                pathComponents[pathComponents.length - 1],
                EntryAttribute.NORMAL_FILE.getValue()
        );
        if (fileEntry == null) {
            return "File does not exist.";
        }

        // 检查文件是否已打开
        if (ofTableManager.find(fileEntry.getStartNum()) != null) {
            return "Cannot display content of an open file.";
        }

        // 从目录中取出文件的起始盘块号
        byte startBlockIndex = fileEntry.getStartNum();
        return this.entryOperator.getAllFromEntry(startBlockIndex);
    }


    /**
     * 改变文件属性
     *
     * @param fileAbsolutePath 文件完整路径
     * @param newAttribute     新的文件属性
     * @return 1:成功， -1:文件不存在，-2：文件重名 ，-6:文件已打开
     */
    public int changeFileAttribute(String fileAbsolutePath, byte newAttribute) throws Exception {
        // 解析文件路径，获取父目录盘块号
        String[] pathComponents = fileAbsolutePath.split("/");
        String parentDirName = String.join("/", Arrays.copyOfRange(pathComponents, 0, pathComponents.length - 1));
        int parentDirBlockIndex = this.entryOperator.findDirBlockIndex(parentDirName);
        // 寻找该登记项
        Entry existingEntry = this.entryOperator.findEntryInDirectory(
                this.entryOperator.findDirBlockIndex(parentDirName),
                pathComponents[pathComponents.length - 1],
                EntryAttribute.NORMAL_FILE.getValue()
        );
        if (existingEntry == null) return -1; // 文件不存在

        // 检查文件是否已打开
        if (ofTableManager.find(existingEntry.getStartNum()) != null) {
            return -6;
        }

        // 改变文件属性
        existingEntry.setAttribute(newAttribute);
        this.entryOperator.setEntryToDirectory(parentDirBlockIndex, existingEntry);
        return 1;
    }
}
