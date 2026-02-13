# 生理期记录应用 - 设计文档

日期: 2025-02-12

---

## CI/CD 工作流

### 分支保护规则

本仓库配置了 GitHub Actions CI 工作流，所有 Pull Request 必须通过以下检查才能合并到 `main` 分支：

| 检查项 | 说明 |
|--------|------|
| Run Unit Tests | 运行所有单元测试 (./gradlew :app:testDebugUnitTest) |
| Run Lint Check | 运行 Android Lint 检查 (./gradlew :app:lintDebug) |
| Check Code Coverage | 生成代码覆盖率报告 (./gradlew :app:jacocoTestReport) |
| Build Debug APK | 构建 Debug APK (./gradlew :app:assembleDebug) |

### 如何配置分支保护

1. 进入 GitHub 仓库 Settings → Branches
2. 点击 Add rule，Branch name pattern 填写 main
3. 勾选以下选项：
   - Require a pull request before merging
   - Require status checks to pass before merging
   - Require branches to be up to date before merging
4. 在 Status checks 中添加：
   - Run Unit Tests
   - Run Lint Check
   - Build Debug APK
5. 点击 Create

### 本地验证

提交 PR 前请在本地运行以下命令验证：

```bash
# 运行所有测试
./gradlew :app:testDebugUnitTest

# 生成覆盖率报告
./gradlew :app:jacocoTestReport

# 运行 Lint 检查
./gradlew :app:lintDebug

# 构建 Debug APK
./gradlew :app:assembleDebug
```

### 测试报告

- CI 会自动上传测试结果、覆盖率报告和 Lint 报告
- 在 PR 页面可以查看覆盖率报告评论
- 测试失败时可在 Actions 页面下载详细报告

---

## 一、功能需求

### 1.1 核心功能

#### 周期记录与预测
- 记录经期开始/结束时间（可修改）
- 记录每日流量（轻/中/重/极少/无）
- 基于加权平均法预测下次经期
- 用户可配置：经期长度、周期长度

#### 症状记录
| 分类 | 具体项目 |
|------|----------|
| 痛经 | 等级记录（0-10）|
| 性行为 | 有无记录 |
| 身体症状 - 全身 | 疲劳、发热、发冷、失眠、嗜睡 |
| 身体症状 - 头部 | 头痛、头晕 |
| 身体症状 - 腹部 | 腹胀、腹泻、便秘、胃疼、恶心 |
| 身体症状 - 皮肤 | 痤疮、皮疹、干燥 |
| 身体症状 - 私处分泌物 | 颜色、质地、气味等 |
| 身体症状 - 其他 | 乳房胀痛、腰痛、关节痛等 |

#### 心情记录
- 可选情绪标签：开心、平静、焦虑、易怒、抑郁、敏感、精力充沛、疲惫

#### 其他记录
- 排卵试纸结果（阴性/弱阳/强阳）
- 自定义文字备注

#### 数据导入导出
- 格式：JSON
- 包含所有历史记录和配置

### 1.2 界面结构

```
├── Tab 1: 主页
│   ├── 顶部：应用标题 + 右上角设置按钮
│   ├── 主体：垂直无限滚动日历
│   │   └── 日期标记：经期（淡粉色）、预测（虚线框）、今天（高亮）
│   └── 点击日期 → 底部弹出记录面板
│
├── Tab 2: 统计
│   ├── 周期历史趋势图
│   ├── 症状统计（出现频率）
│   ├── 平均周期长度
│   └── 经期规律分析
│
└── 设置页面（从主页右上角进入）
    ├── 周期配置（经期长度、周期长度）
    ├── 数据导入
    ├── 数据导出
    └── 关于
```

---

## 二、技术架构

### 2.1 技术栈

| 层级 | 技术 |
|------|------|
| UI | Jetpack Compose |
| 架构模式 | MVVM + Repository |
| 依赖注入 | Hilt |
| 本地存储配置 | DataStore Preferences |
| 本地存储数据 | Room (SQLite) |
| 异步处理 | Kotlin Coroutines + Flow |
| 图表 | Compose Charts |

### 2.2 数据模型

#### 配置数据 (DataStore)
```kotlin
data class UserSettings(
    val periodLength: Int,      // 经期长度（天），默认 5
    val cycleLength: Int,       // 周期长度（天），默认 28
    val themeColor: String      // 主题色，默认粉色
)
```

#### 每日记录 (Room Entity)
```kotlin
@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey
    val date: LocalDate,           // 日期
    val isPeriodDay: Boolean,      // 是否经期日
    val flowLevel: FlowLevel?,     // 流量级别
    val painLevel: Int?,           // 0-10
    val hadSex: Boolean,
    val physicalSymptoms: List<Symptom>,
    val mood: Mood?,
    val ovulationTest: OvulationResult?,
    val note: String?
)
```

### 2.3 预测算法

加权平均法实现，参考上面代码示例。

---

## 三、UI 设计规范

### 3.1 配色方案

| 用途 | 颜色值 |
|------|--------|
| 主题色（主）| #F8BBD9 (淡粉色) |
| 主题色（深）| #F48FB1 |
| 经期标记 | #F48FB1 |
| 预测标记 | #F8BBD9 (虚线边框) |
| 背景色 | #FFF5F7 |
| 文字主色 | #333333 |
| 文字次色 | #888888 |

---

## License

This project is open-sourced under the MIT License.
