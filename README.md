# CardTally

一个基于 Material Design 风格的现代化 Android 记账应用，帮助用户轻松管理个人财务，提供直观的数据分析和优雅的用户界面。

## ✨ 功能特性

- **Material Design 风格**：采用 Google 官方设计规范，界面美观、简洁、易用
- **快速记账**：支持快速添加收支记录，支持分类、描述等详情
- **分类管理**：自定义收支分类，支持增删改查
- **统计分析**：按分类和时间统计收支情况
- **主题切换**：支持浅色、深色、跟随系统三种主题
- **快捷记账**：可选择启动时直接进入记账界面
- **记录管理**：支持编辑和删除已有记录
- **数据安全**：本地 SQLite 数据库存储，保护个人财务隐私

## 🛠️ 技术栈

- **开发语言**：Kotlin
- **最低 SDK**：Android 7.0 (API 24)
- **目标 SDK**：Android 14 (API 34)
- **UI 框架**：Material Design Components
- **架构组件**：Fragment, ViewModel
- **数据库**：SQLite
- **构建工具**：Gradle 9.0.0

## 📦 安装步骤

### 1. 克隆仓库

```bash
git clone https://github.com/zhiwk/CardTally.git
cd CardTally
```

### 2. 构建项目

```bash
# Windows
./gradlew.bat assembleDebug

# Mac/Linux
./gradlew assembleDebug
```

### 3. 安装应用

将生成的 APK 文件安装到 Android 设备：

```
app/build/outputs/apk/debug/app-debug.apk
```

## 📁 项目结构

```
CardTally/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/cardtally/
│           │   ├── adapter/          # RecyclerView 适配器
│           │   ├── database/         # 数据库帮助类
│           │   ├── model/            # 数据模型类
│           │   ├── util/             # 工具类
│           │   ├── MainActivity.kt   # 主活动
│           │   ├── HomeFragment.kt   # 首页
│           │   ├── AddRecordFragment.kt    # 添加记录
│           │   ├── EditRecordFragment.kt   # 编辑记录
│           │   ├── StatisticsFragment.kt   # 统计分析
│           │   ├── SettingsFragment.kt     # 设置页面
│           │   ├── CategoryManageFragment.kt  # 分类管理
│           │   └── ThemeSettingsFragment.kt   # 主题设置
│           ├── res/
│           │   ├── layout/           # 布局文件
│           │   ├── values/           # 字符串、颜色等资源
│           │   ├── drawable/         # 图标资源
│           │   └── menu/             # 菜单资源
│           └── AndroidManifest.xml
├── build.gradle                      # 项目级构建配置
└── README.md                         # 项目说明
```

## 🚀 使用指南

### 快速记账
1. 打开应用，点击右下角的「+」按钮进入记账页面
2. 选择收支类型（收入/支出）
3. 输入金额、选择分类、添加描述（可选）
4. 点击「保存」完成记账

### 查看统计分析
1. 进入「统计」页面
2. 查看总支出和总收入概览
3. 切换「按分类统计」或「按时间统计」
4. 查看详细的统计数据

### 管理分类
1. 进入「设置」页面
2. 点击「分类管理」
3. 切换支出/收入标签页
4. 点击「添加分类」创建新分类，或编辑/删除现有分类

### 切换主题
1. 进入「设置」页面
2. 点击「主题设置」
3. 选择浅色主题、深色主题或跟随系统
4. 重启应用生效

### 快捷记账设置
1. 进入「设置」页面
2. 打开「是否快捷记账」开关
3. 下次启动应用将直接进入记账界面

### 编辑/删除记录
1. 在首页找到要操作的记录
2. 点击「编辑」按钮修改记录信息
3. 点击「删除」按钮删除记录

## 🎨 主题预览

### 浅色主题
- 主色：紫色 (#6200EE)
- 背景：浅灰色 (#FAFAFA)

### 深色主题
- 主色：浅紫色 (#BB86FC)
- 背景：深灰色 (#121212)

### 跟随系统
- 主色：蓝色 (#1976D2)
- 根据系统设置自动切换明暗模式

## 📊 默认分类

### 支出分类
餐饮、交通、购物、娱乐、医疗、教育、住房、其他

### 收入分类
工资、奖金、投资、兼职、其他

## 🤝 贡献指南

欢迎各位开发者贡献代码或提出建议！

1. Fork 本仓库
2. 创建分支：`git checkout -b feature/your-feature`
3. 提交修改：`git commit -m 'Add some feature'`
4. 推送分支：`git push origin feature/your-feature`
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 📞 联系方式

- 项目地址：[https://github.com/zhiwk/CardTally](https://github.com/zhiwk/CardTally)
- 如有问题或建议，欢迎提交 Issue 或联系开发者

---

**CardTally** - 让记账变得简单而优雅 ✨
