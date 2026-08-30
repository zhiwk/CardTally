# CardTally

一个基于 Material Design 风格的现代化 Android 记账应用，帮助用户轻松管理个人财务，提供直观的数据分析和优雅的用户界面。

## ✨ 功能特性

- **Material Design 风格**：采用 Google 官方设计规范，界面美观、简洁、易用
- **快速记账**：支持快速添加收支记录，支持分类、描述等详情
- **分类管理**：自定义收支分类，支持增删改查
- **统计分析**：按分类和时间统计收支情况
- **主题切换**：支持浅色、深色、跟随系统三种主题；历史彩色主题残留已移除，旧彩色主题存档值会自动回退
- **快捷记账**：可选择启动时直接进入记账界面
- **记录管理**：支持编辑和删除已有记录
- **数据安全**：本地 SQLite 数据库存储，保护个人财务隐私
- **AI 助手**：支持用户自填 MiniMax 配置（API Key / 模型 / 请求 URL），并在 AI 助手页进行本地发起的 MiniMax 流式文本对话；当服务端返回普通 JSON 时也会自动按非流式结果处理；当前已支持本地 SQLite 持久化多会话、会话切换、新建与重命名

## 🛠️ 技术栈

- **开发语言**：Kotlin
- **最低 SDK**：Android 7.0 (API 24)
- **目标 / 编译 SDK**：Android 14 (API 34)
- **UI 框架**：XML 布局 + Material Components + Fragment
- **数据层**：SQLite（`DatabaseHelper.kt`）
- **网络层**：OkHttp（用于 MiniMax BYOK 文本聊天）
- **构建工具**：Android Gradle Plugin 8.3.0 + Gradle Wrapper 8.13

## 📦 安装步骤

### 1. 克隆仓库

```bash
git clone https://github.com/zhiwk/CardTally.git
cd CardTally
```

### 2. 构建项目

```bash
# Windows (PowerShell)
.\gradlew.bat assembleDebug

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
│           │   └── *Fragment.kt      # 页面级逻辑，如 Home / AddRecord / EditRecord / AddAsset / EditAsset / Asset / Statistics / Settings / Search / Agent 等
│           ├── res/
│           │   ├── layout/           # 布局文件
│           │   ├── values/           # 中文字符串、颜色、尺寸、样式等资源
│           │   ├── values-en/        # 英文字符串资源
│           │   ├── color/            # selector 等颜色状态资源
│           │   ├── drawable/         # 图标资源
│           │   ├── menu/             # 菜单资源
│           │   └── xml/              # locale_config 等 XML 配置
│           └── AndroidManifest.xml
├── docs/                             # 文档目录（见下文）
├── skills/                           # 本地协作 skill（编译 / 安装 / 截图等）
├── screenshot/                       # 真机截图留档目录（按需生成）
├── AGENTS.md                         # 仓库协作入口
├── build.gradle                      # 项目级构建配置
└── README.md                         # 项目说明
```

## 📚 文档入口

```
docs/
├── requirements/      # 需求文档
│   ├── plans/       # 实施计划
│   └── decisions/   # 业务决策
├── design/          # 设计文档
│   ├── guidelines/  # 设计指南
│   └── assets/      # 设计产出物
├── collaboration/    # AI协作指南
│   └── skills/      # 拆分协作文档中的专项 skill
└── archive/         # 历史归档
```

- `AGENTS.md`：仓库协作入口
- `skills/`：根目录本地协作 skill

## 🚀 使用指南

### 快速记账
1. 打开应用，点击右下角的「+」按钮进入记账页面
2. 在极简录入页中选择收支类型（收入/支出）并输入金额
3. 点击信息卡中的日期、资产、分类行，分别通过底部抽屉完成选择
4. 分类抽屉支持树形层级展开，但当前记录仍只能选择叶子分类
5. 添加备注（可选）后，点击底部「保存」完成记账

### 查看账单与统计
1. 「账单」页默认显示当前账本、本月统计和当月明细
2. 进入「统计」页查看分类、周期和趋势图表
3. 账单明细可按日期浏览记录，并继续编辑或删除记录

### 管理分类
1. 进入「设置」页面
2. 点击「分类管理」
3. 切换支出/收入标签页
4. 点击「添加分类」创建新分类，或编辑/删除现有分类

### 切换主题
1. 进入「设置」页面
2. 点击「主题设置」
3. 选择浅色主题、深色主题或跟随系统
4. 当前仓库已开始将旧的浅色硬编码资源收口为主题属性 / 夜间覆盖，深色模式不再只在 AI 页面生效
5. 重启应用生效

### 快捷记账设置
1. 进入「设置」页面
2. 打开「是否快捷记账」开关
3. 下次启动应用将直接进入记账界面

### 配置 AI 助理
1. 进入「我的」页面
2. 在「AI 助理」分组中打开「显示 AI 入口」开关
3. 点击「API 配置」进入二级页面并保存本机 `API Key / 模型 / 请求 URL`
4. 进入「AI 助手」页后，左上角菜单可切换历史会话，长按会话可重命名；右上角 `+` 可新建会话，默认命名为“新会话-年月日”
5. 模型回复会在支持流式返回时增量显示；若接口返回普通 JSON，则会自动回退为一次性展示完整回复
6. 当前会话与消息会保存在本地 SQLite 中，切换页面或重启应用后仍会保留，并自动恢复上次活动会话
7. 会话列表展开时，底部导航会随 AI 助手页面临时隐藏，关闭抽屉后恢复显示
8. 关闭该开关后，底部「AI 助手」标签会隐藏

### 一级页与二级页导航规则
1. 账单 / 统计 / 资产 / AI 助手 / 我的 属于一级页，显示底部导航
2. 进入二级页面（如记一笔、编辑记录、API 配置、主题设置等）后，底部导航会隐藏
3. 返回一级页后，底部导航会恢复显示
4. 在 AI 助手页展开会话抽屉时，底部导航也会临时隐藏，关闭抽屉后恢复

### 编辑/删除记录
1. 在账本明细中找到要操作的记录
2. 点击记录进入「编辑记录」页；编辑页与「记一笔」共用同一套抽屉式录入界面
3. 修改金额、日期、资产、分类或备注后保存
4. 如需删除，可在账本明细中使用现有删除交互删除记录

## 🎨 主题预览

### 浅色主题
- 主色：静奢灰紫 (#615d66)
- 背景：暖白 (#fffcf7)

### 深色主题
- 主色：雾蓝灰 (#B8C8D9)
- 背景：深夜蓝黑 (#101518)

### 跟随系统
- 跟随系统设置自动切换明暗模式
- 保持当前应用的「静奢理财日记」主题配色体系

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
