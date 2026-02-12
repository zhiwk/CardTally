# CardTally

一个基于 Material Design 风格的现代化记账软件，帮助用户轻松管理个人财务，提供直观的数据分析和优雅的用户界面。

## ✨ 功能特性

- **Material Design 风格**：采用 Google 官方设计规范，界面美观、简洁、易用
- **快速记账**：支持快速添加收支记录，支持分类、标签、备注等详情
- **数据可视化**：通过图表直观展示收支趋势、分类占比等财务数据
- **分类管理**：自定义收支分类，支持图标和颜色标识
- **预算管理**：设置月度/年度预算，实时跟踪预算使用情况
- **数据导出**：支持导出 Excel、CSV 等格式的财务数据
- **多设备同步**：云端存储，支持多设备数据同步（可选）
- **数据安全**：本地加密存储，保护个人财务隐私

## 🛠️ 技术栈

- **前端框架**：Vue 3 / React（根据项目实际选择）
- **状态管理**：Pinia / Redux（根据项目实际选择）
- **UI 库**：Material-UI / Vuetify（实现 Material Design 风格）
- **图表库**：ECharts / Chart.js（数据可视化）
- **数据存储**：LocalStorage / IndexedDB（本地存储）+ 可选云端存储
- **构建工具**：Vite / Webpack
- **包管理**：npm / yarn

## 📦 安装步骤

### 1. 克隆仓库

```bash
git clone https://github.com/zhiwk/CardTally.git
cd CardTally
```

### 2. 安装依赖

```bash
# 使用 npm
npm install

# 或使用 yarn
yarn install
```

### 3. 开发环境运行

```bash
# 使用 npm
npm run dev

# 或使用 yarn
yarn dev
```

### 4. 构建生产版本

```bash
# 使用 npm
npm run build

# 或使用 yarn
yarn build
```

构建产物将生成在 `dist` 目录中，可部署到静态网站托管服务。

## 📁 项目结构

```
CardTally/
├── public/              # 静态资源
├── src/
│   ├── assets/          # 图片、图标等资源
│   ├── components/      # 可复用组件
│   ├── views/           # 页面视图
│   ├── router/          # 路由配置
│   ├── store/           # 状态管理
│   ├── utils/           # 工具函数
│   ├── services/        # 服务（如数据存储、API 调用）
│   ├── styles/          # 全局样式
│   └── main.js          # 应用入口
├── index.html           # HTML 模板
├── package.json         # 项目配置
├── vite.config.js       # Vite 配置（或 webpack.config.js）
└── README.md            # 项目说明
```

## 🚀 使用指南

### 快速记账
1. 打开应用，点击「+」按钮进入记账页面
2. 选择收支类型（收入/支出）
3. 输入金额、选择分类、添加标签和备注
4. 点击「保存」完成记账

### 查看统计分析
1. 进入「统计」页面
2. 选择时间范围（周/月/年）
3. 查看收支趋势图、分类占比图等数据
4. 点击具体分类可查看详细记录

### 管理分类
1. 进入「设置」页面
2. 选择「分类管理」
3. 点击「添加分类」创建新分类，或编辑/删除现有分类
4. 为分类选择图标和颜色，便于识别

### 设置预算
1. 进入「预算」页面
2. 点击「添加预算」
3. 选择预算类型（月度/年度）、设置金额和时间范围
4. 保存后可在首页查看预算使用进度

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