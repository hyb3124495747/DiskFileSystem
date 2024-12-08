# DiskFileSystem

#### 介绍
文件管理系统

#### 软件架构
###### 设计了分层的软件架构，其中每一层都向上层提供接口，同时依赖于下层的实现。这种设计有助于实现解耦合，使得各层可以独立变化，而不影响其他层。

- Entity 层
    - 作用：定义系统中的核心实体，如 Entry, File, OFLE 等。
    - 解耦合：实体层只关心数据的结构和状态，不涉及业务逻辑或数据存取逻辑。
- Enum 层
    - 作用：定义枚举类型，如 BlockStatus, EntryAttribute 等，用于系统中的各种状态和属性。
    - 解耦合：通过枚举类型，提供了一组命名的常量，避免了数字的直接使用，使得代码更加清晰和易于维护。
- Manager 层
    - 作用：管理资源和业务逻辑， DiskManager, OFTableManager。
    - 解耦合：这一层封装了对底层资源（如磁盘块管理）的操作，向上层提供操作这些资源的方法。
- Service 层
    - 作用：封装业务逻辑，EntryOperator, FileDirOperator, DirOperator， FileSystem。
    - 解耦合：这一层EntryOperator使用 Manager 层提供的接口来实现具体的业务逻辑，
    - 然后FileDirOperator和DirOperator再利用EntryOperator提供的功能进行进一步业务逻辑的设计，不直接依赖于具体的数据存取和资源管理细节，
    - 最后FileSystem调用FileDirOperator和DirOperator的各种方法分别对文件或目录进行操作。
- Controller 层
    - 作用：处理用户输入，调用FileSystem的方法，并返回结果。
    - 解耦合：Controller 层作为用户界面和后端逻辑之间的桥梁，它不包含业务逻辑，而是调用 Service 层的方法。



#### 安装教程
- 没有教程

#### 使用说明
- 运行 src/main/java/application/Main.java

