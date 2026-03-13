# HealthShow 项目开发记录

> 本文件记录重要的 Bug 修复、经验教训和最佳实践，不会被自动覆盖。

---

## 2026-03-13：Dashboard 卡路里数据显示修复

**问题**：Dashboard 体征卡片中"人均卡路里"显示 "-- kcal"

### 排查过程（完整数据流验证）

1. **DashboardMapper.java SQL查询** ✅ - 已有 `AVG(CAST(calories AS FLOAT)) AS avgCalories`
2. **DashboardServiceImpl.java 返回** ✅ - 已有 `result.put("avgCalories", ...)`
3. **前端 dashboard/index.vue** ✅ - 正确使用 `b.avgCalories`
4. **数据库数据验证** ❌ - `SELECT calories FROM health_record_202603` 全是 NULL
5. **后端日志验证** ✅ - 日志显示接收到 calories 数据（如 `calories=580`）

### 根本原因

- **HealthRecordMapper.java** 的 `insertToTable()` SQL 语句**缺少 calories 字段**
- 后端接收到数据，DataProcessService 处理正确，但 INSERT 语句没有写入 calories
- **v_health_record 视图**也缺少 calories 列（13个月表 UNION ALL 均未包含）

### 修复方案（3处）

1. **HealthRecordMapper.java** (lines 42-43, 55-56):
   ```java
   // 字段列表添加
   "  <if test='r.calories != null'>calories,</if>" +
   // VALUES 列表添加
   "  <if test='r.calories != null'>#{r.calories},</if>" +
   ```

2. **v_health_record 视图** - DROP + CREATE，所有13个月表的 SELECT 添加 calories 列：
   ```sql
   SELECT id, user_code, ..., calories, record_time, ...
   FROM health_record_202601 UNION ALL
   SELECT id, user_code, ..., calories, record_time, ...
   FROM health_record_202602 UNION ALL
   ... (共13个月表)
   ```

3. **后端完整重启** - MyBatis Mapper 修改需完整重启（JRebel 无法热重载）

### 验证结果

- ✅ 数据库写入成功：`calories = 220, 302, 351, 367...`
- ✅ 视图查询成功：`SELECT AVG(calories) = 441`
- ✅ Dashboard API 返回 `avgCalories = 441`
- ✅ 前端显示"441 kcal"

---

## 🚨 关键教训与最佳实践

### 1. 添加新字段的完整检查清单（CRITICAL）

当添加新的健康数据字段时，**必须完整检查以下所有步骤**：

- [ ] **数据库表结构**：ALTER TABLE 主表 + 13个分区表（health_record_202601 ~ 202701）
- [ ] **视图定义**：DROP + CREATE v_health_record，所有 UNION ALL 分支包含新字段
- [ ] **实体类**：HealthRecord.java 添加字段 + @TableField 注解
- [ ] **Mapper INSERT**：HealthRecordMapper.insertToTable() 的字段列表和 VALUES 列表
- [ ] **Mapper SELECT**：所有相关查询（如 DashboardMapper.getAverageByRange）
- [ ] **Service 层返回**：DashboardServiceImpl 等 Service 的 result.put()
- [ ] **前端 API 使用**：dashboard/index.vue 等前端页面的数据绑定
- [ ] **数据流完整验证**：模拟器 → 后端日志 → 数据库写入 → 视图查询 → API 返回 → 前端显示

**遗漏任何一步都会导致数据丢失或显示异常！**

### 2. 完整重启 vs 热重载判断规则

- **必须完整重启**：
  - MyBatis Mapper SQL 修改（@Select/@Insert/@Update/@Delete）
  - 视图/表结构变更（需重启清除连接池缓存）
  - application.yml 配置文件修改
  - 新增 Mapper 方法到接口

- **JRebel 可热重载**：
  - Service/Controller/Model Java 代码修改
  - 修改现有方法的业务逻辑
  - 添加新的 Service/Controller 方法（非 Mapper）

### 3. Bug 排查的验证顺序（从底层到上层）

**CRITICAL - 必须按此顺序验证，NEVER 跳过步骤**：

1. **数据库原始数据**：
   ```sql
   -- 检查分区表数据
   SELECT TOP 5 * FROM health_record_202603
   WHERE CAST(record_time AS DATE) = '2026-03-13'
   ORDER BY record_time DESC
   ```

2. **后端接收日志**：
   ```bash
   tail -f backend.log | grep "calories"
   # 确认后端是否接收到数据
   ```

3. **视图查询**：
   ```sql
   -- 检查视图是否包含新字段
   SELECT AVG(calories) FROM v_health_record WHERE ...
   ```

4. **API 返回**：
   ```bash
   curl "http://localhost:8080/health/dashboard/body-indicators?..."
   # 检查 JSON 响应是否包含 avgCalories
   ```

5. **前端显示**：
   - 刷新浏览器页面
   - 打开开发者工具检查 Network 和 Console

**⚠️ NEVER 先告诉用户"可以刷新了"再验证！必须完整验证所有步骤后才通知用户。**

### 4. sqlcmd 常用验证命令

```bash
# 查看表结构
sqlcmd -S "R9000K3080\MSSQLSERVER2019" -E -d health \
  -Q "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'health_record_202603'"

# 查看视图列
sqlcmd -S "R9000K3080\MSSQLSERVER2019" -E -d health \
  -Q "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'v_health_record'"

# 查询数据（带格式）
sqlcmd -S "R9000K3080\MSSQLSERVER2019" -E -d health \
  -Q "SELECT TOP 5 record_time, calories FROM health_record_202603 WHERE calories IS NOT NULL" \
  -W -s","
```

---

## 项目架构要点

- **数据写入路径**：模拟器 → Netty TCP → DataProcessService → RedisHealthBufferService → HealthRecordMapper.insertToTable → 月度分区表
- **数据读取路径**：Controller → Service → Mapper → v_health_record 视图 → UNION ALL 13个月表
- **分区表命名**：health_record_YYYYMM（如 health_record_202603）
- **视图作用**：v_health_record 统一查询入口，避免业务代码关心分表逻辑

---

> **更新时间**：2026-03-13
> **更新人**：Claude Code
> **下次修复时**：先完整验证所有步骤，再通知用户！
