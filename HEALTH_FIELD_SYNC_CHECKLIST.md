# Health Field Sync Checklist

新增健康字段时，不要只改实体或某一个页面。这个仓库目前最容易出问题的是“分表直查 SQL”和“插入 SQL”漏字段，导致接口能跑但数据不完整。

## 自动检查

先跑：

```powershell
python scripts/check_health_field_sync.py
```

这条检查会把 `HealthRecord.java` 里的当前指标字段当作源头，核对它们是否继续出现在以下关键文件中：

- `HealthRecordMapper.java` 的 `insertToTable()`
- `DashboardServiceImpl.java` 的分区表拼接 SQL
- `HeartRateServiceImpl.java` 的宽表分区源 SQL

它能挡住最常见的“实体加了字段，但 INSERT / 直查源忘了加”的回归。

## 手工检查

下面这些仍然必须人工确认：

1. 月分表结构是否已补字段。
2. `v_health_record` / `v_warning_record` 视图是否包含新字段。
3. `sp_update_monthly_views` 是否同步更新。
4. 相关 Mapper / Service 返回值是否把新字段暴露出来。
5. 前端 `src/api` 和实际页面绑定是否同步。
6. 设备/模拟器 -> 日志 -> 数据库 -> API -> 页面 的整链路是否验证。

## 推荐收口命令

后端：

```powershell
& 'C:\Program Files\Java\jdk-17\bin\java.exe' -version
mvn -q -DskipTests compile
python scripts/check_health_field_sync.py
```

前端：

```powershell
cd ..\HealthShow
npm run audit:api
npm run audit:e2e
npm run build
```
