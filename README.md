# 生理期记录应用 - 设计文档

日期: 2025-02-12

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
| 图表 | Compose Charts 或 MPAndroidChart Compose |

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

    // 经期相关
    val isPeriodDay: Boolean,      // 是否经期日
    val flowLevel: FlowLevel?,     // 流量级别

    // 痛经
    val painLevel: Int?,           // 0-10

    // 性行为
    val hadSex: Boolean,

    // 身体症状（使用位掩码或序列化列表）
    val physicalSymptoms: List<Symptom>,

    // 心情
    val mood: Mood?,

    // 其他
    val ovulationTest: OvulationResult?,
    val note: String?
)
```

#### 周期记录 (用于预测算法)
```kotlin
@Entity(tableName = "periods")
data class Period(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate?      // null 表示仍在经期中
)
```

### 2.3 预测算法

**加权平均法实现：**

```kotlin
fun predictNextPeriod(periods: List<Period>, cycleLengthSetting: Int): LocalDate {
    if (periods.isEmpty()) {
        // 无历史数据，使用设置值
        return LocalDate.now().plusDays(cycleLengthSetting.toLong())
    }

    val recentPeriods = periods.takeLast(6)  // 最近6个周期
    val cycleLengths = recentPeriods.zipWithNext { a, b ->
        ChronoUnit.DAYS.between(a.startDate, b.startDate).toInt()
    }

    if (cycleLengths.isEmpty()) {
        return periods.last().startDate.plusDays(cycleLengthSetting.toLong())
    }

    // 加权平均：越近的周期权重越高
    val weightedSum = cycleLengths.reversed().mapIndexed { index, length ->
        length * (index + 1)
    }.sum()
    val weightsSum = (1..cycleLengths.size).sum()
    val averageCycle = weightedSum / weightsSum

    return periods.last().startDate.plusDays(averageCycle.toLong())
}
```

---

## 三、UI 设计规范

### 3.1 配色方案

| 用途 | 颜色值 |
|------|--------|
| 主题色（主）| `#F8BBD9` (淡粉色) |
| 主题色（深）| `#F48FB1` |
| 经期标记 | `#F48FB1` |
| 预测标记 | `#F8BBD9` (虚线边框) |
| 背景色 | `#FFF5F7` |
| 文字主色 | `#333333` |
| 文字次色 | `#888888` |

### 3.2 组件规范

#### 日历单元格
- 尺寸：正方形，自适应宽度
- 经期日：淡粉色背景填充
- 预测日：淡粉色虚线边框
- 今天：深色边框高亮

#### 记录面板
- 类型：BottomSheet
- 高度：屏幕 70%
- 内部：垂直滚动表单

---

## 四、项目结构

```
app/src/main/java/com/example/menstruation/
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── DailyRecordDao.kt
│   │   │   └── PeriodDao.kt
│   │   ├── datastore/
│   │   │   └── SettingsDataStore.kt
│   │   └── entity/
│   │       ├── DailyRecordEntity.kt
│   │       └── PeriodEntity.kt
│   ├── repository/
│   │   ├── PeriodRepository.kt
│   │   └── SettingsRepository.kt
│   └── model/
│       ├── DailyRecord.kt
│       ├── Period.kt
│       ├── Symptom.kt
│       ├── Mood.kt
│       └── UserSettings.kt
├── domain/
│   ├── usecase/
│   │   ├── PredictNextPeriodUseCase.kt
│   │   ├── GetCalendarDataUseCase.kt
│   │   └── ExportDataUseCase.kt
│   └── util/
│       └── DateUtils.kt
├── ui/
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   ├── components/
│   │   │   ├── CalendarView.kt
│   │   │   └── RecordBottomSheet.kt
│   │   └── state/
│   │       └── HomeUiState.kt
│   ├── stats/
│   │   ├── StatsScreen.kt
│   │   ├── StatsViewModel.kt
│   │   └── components/
│   │       └── CycleChart.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── components/
│       └── CommonComponents.kt
├── di/
│   └── AppModule.kt
└── MainActivity.kt
```

---

## 五、关键实现细节

### 5.1 日历视图

使用 LazyColumn 实现垂直无限滚动日历：
- 以当前月份为中心
- 向上滚动加载历史月份
- 向下滚动加载未来月份
- 每个月份显示为独立的月份组件

### 5.2 导入导出格式

```json
{
  "version": 1,
  "exportDate": "2025-02-12T10:30:00Z",
  "settings": {
    "periodLength": 5,
    "cycleLength": 28,
    "themeColor": "#F8BBD9"
  },
  "periods": [
    {
      "startDate": "2025-01-15",
      "endDate": "2025-01-19"
    }
  ],
  "dailyRecords": [
    {
      "date": "2025-01-15",
      "isPeriodDay": true,
      "flowLevel": "medium",
      "painLevel": 3,
      "hadSex": false,
      "physicalSymptoms": ["bloating", "fatigue"],
      "mood": "irritable",
      "note": ""
    }
  ]
}
```

---

## 六、开发优先级

### Phase 1: 核心功能
1. 数据库和 DataStore 设置
2. 配置页面（经期长度、周期长度）
3. 基础日历视图
4. 记录经期开始/结束
5. 预测算法

### Phase 2: 记录功能
1. 流量记录
2. 痛经记录
3. 症状记录
4. 心情记录
5. 排卵试纸记录
6. 备注

### Phase 3: 统计与数据
1. 统计页面
2. 数据导出
3. 数据导入

### Phase 4: 优化
1. 动画效果
2. 性能优化
3. 边界情况处理

---

## 七、验收标准

- [ ] 可以正确记录经期并预测下次时间
- [ ] 可以记录所有症状类型
- [ ] 日历可以无限垂直滚动
- [ ] 点击日期可以弹出记录面板
- [ ] 统计数据准确
- [ ] JSON 导入导出功能正常
- [ ] 界面使用淡粉色主题

---

# 实现计划

## Phase 1: 项目基础架构（2-3小时）

### 目标
配置依赖项、数据库、DataStore、主题。

### 文件变更
1. `gradle/libs.versions.toml` - 添加 Room、Hilt、DataStore、Navigation 版本
2. `build.gradle.kts` (project) - 添加 KSP 和 Hilt 插件
3. `app/build.gradle.kts` - 添加所有依赖
4. `MenstruationApp.kt` - 创建 Application 类
5. `Color.kt` / `Theme.kt` - 设置淡粉色主题
6. `DailyRecordEntity.kt` / `PeriodEntity.kt` - 数据库实体
7. `AppDatabase.kt` / `Converters.kt` - Room 数据库
8. `DailyRecordDao.kt` / `PeriodDao.kt` - DAO 接口
9. `SettingsDataStore.kt` - DataStore 配置
10. `AppModule.kt` - Hilt DI 模块
11. 数据模型类 - `DailyRecord.kt`, `Period.kt`, `UserSettings.kt`, `Symptom.kt`, `Mood.kt` 等
12. `PeriodRepository.kt` / `SettingsRepository.kt` - Repository 层

### 验收标准
- [ ] 项目成功编译
- [ ] Room 数据库配置正确
- [ ] DataStore 配置正确
- [ ] Hilt DI 配置正确

---

## Phase 2: 核心功能（4-6小时）

### 目标
实现日历视图、经期记录、预测算法。

### 文件变更
1. `PredictNextPeriodUseCase.kt` - 加权平均法预测算法
2. `CalendarView.kt` - 垂直无限滚动日历组件
3. `RecordBottomSheet.kt` - 记录面板 Bottom Sheet
4. `HomeScreen.kt` - 主页（日历 + Tab 导航）
5. `HomeViewModel.kt` - 主页状态管理
6. `HomeUiState.kt` - UI 状态类

### 关键功能
- 垂直无限滚动日历（LazyColumn）
- 日期标记：经期（淡粉色）、预测（虚线边框）、今天（高亮）
- 点击日期弹出记录面板
- 经期开始/结束按钮
- 加权平均法预测（最近6个周期）

### 验收标准
- [ ] 日历可以垂直无限滚动
- [ ] 点击日期弹出记录面板
- [ ] 可以标记经期开始/结束
- [ ] 预测算法正确计算下次经期

---

## Phase 3: 完整记录功能（3-4小时）

### 目标
实现所有症状和心情记录。

### 文件变更
- `FlowLevelSection.kt` - 流量选择（轻/中/重/极少/无）
- `PainLevelSection.kt` - 痛经滑动条（0-10）
- `SexSection.kt` - 性行为开关
- `SymptomsSection.kt` - 身体症状多选（6大分类）
- `MoodSection.kt` - 心情单选
- `OvulationSection.kt` - 排卵试纸结果
- `NoteSection.kt` - 备注文本框

### 症状分类
- 全身：疲劳、发热、发冷、失眠、嗜睡
- 头部：头痛、头晕
- 腹部：腹胀、腹泻、便秘、胃疼、恶心
- 皮肤：痤疮、皮疹、干燥
- 私处分泌物：颜色、质地
- 其他：乳房胀痛、腰痛、关节痛

### 验收标准
- [ ] 所有症状可以正确记录
- [ ] 症状分类显示清晰
- [ ] 数据正确保存到数据库
- [ ] 重新打开面板时显示已保存的数据

---

## Phase 4: 统计页面（2-3小时）

### 目标
实现统计图表功能。

### 文件变更
1. `StatsScreen.kt` - 统计页面
2. `StatsViewModel.kt` - 统计逻辑
3. `CycleLengthChart.kt` - 周期长度趋势图
4. `AverageCycleCard.kt` - 平均周期统计
5. `SymptomStatsCard.kt` - 症状统计
6. `MoodStatsCard.kt` - 心情统计

### 验收标准
- [ ] 显示周期长度趋势图
- [ ] 显示平均周期长度
- [ ] 显示症状统计
- [ ] 显示心情统计

---

## Phase 5: 导入导出功能（2-3小时）

### 目标
实现 JSON 格式的数据导入导出。

### 文件变更
1. `ExportDataUseCase.kt` - 导出数据
2. `ImportDataUseCase.kt` - 导入数据
3. `ExportData.kt` - 导出数据模型
4. `SettingsScreen.kt` - 导入/导出按钮和文件选择

### JSON 格式
```json
{
  "version": 1,
  "exportDate": "2025-02-12T10:30:00Z",
  "settings": { "periodLength": 5, "cycleLength": 28 },
  "periods": [{ "startDate": "2025-01-15", "endDate": "2025-01-19" }],
  "dailyRecords": [{ "date": "2025-01-15", "isPeriodDay": true, ... }]
}
```

### 验收标准
- [ ] 可以导出所有数据为 JSON
- [ ] 可以从 JSON 导入数据
- [ ] 导入时验证数据格式
- [ ] 导入成功/失败有提示

---

## 时间估算

| Phase | 预估时间 |
|-------|----------|
| Phase 1: 基础架构 | 2-3 小时 |
| Phase 2: 核心功能 | 4-6 小时 |
| Phase 3: 记录功能 | 3-4 小时 |
| Phase 4: 统计页面 | 2-3 小时 |
| Phase 5: 导入导出 | 2-3 小时 |
| **总计** | **13-19 小时** |

---

## 风险点

| 风险 | 级别 | 缓解措施 |
|------|------|----------|
| 日历无限滚动性能 | 高 | 使用 LazyColumn，只渲染可见月份 |
| Room 数据库迁移 | 中 | 初始设计完善字段，减少后续修改 |
| 预测算法准确性 | 中 | 加权平均法，用户提供初始值 |

---

**计划已就绪，等待开始实施。**
