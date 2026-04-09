package com.xzkj.health.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

/**
 * 月份表自动管理调度器
 *
 * 职责：
 *   1. 应用启动时（@PostConstruct）确保当前月和下个月的分表已存在
 *   2. 每月1号凌晨1点（@Scheduled）自动创建下个月的分表并刷新视图
 *
 * 依赖的数据库存储过程（需提前执行 monthly_partition_sql.sql 创建）：
 *   - sp_create_monthly_tables(@year, @month)  — 创建指定月份的 health/warning 月表
 *   - sp_update_monthly_views                  — 重建 v_health_record / v_warning_record 视图
 */
@Slf4j
@Component
public class MonthlyTableScheduler {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 应用启动后立即执行：确保当前月和下个月的分表及视图已就绪。
     * 存储过程内部会检查表是否已存在（IF NOT EXISTS），重复执行安全。
     */
    @PostConstruct
    public void ensureTablesOnStartup() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime next = now.plusMonths(1);

            log.info("[分表] 启动检查：创建 {}-{} 月份分表", now.getYear(), now.getMonthValue());
            createTablesForMonth(now.getYear(), now.getMonthValue());

            log.info("[分表] 启动检查：预创建 {}-{} 月份分表", next.getYear(), next.getMonthValue());
            createTablesForMonth(next.getYear(), next.getMonthValue());

            log.info("[分表] 刷新视图 v_health_record / v_warning_record");
            updateViews();

            log.info("[分表] 启动检查完成");
        } catch (Exception e) {
            // 不中断启动，但要打印异常，方便排查（如存储过程尚未创建）
            log.error("[分表] 启动检查失败（请先执行 monthly_partition_sql.sql）: {}", e.getMessage());
        }
    }

    /**
     * 每月1号凌晨1点执行：
     *   1. 创建下个月的 health_record_YYYYMM / warning_record_YYYYMM
     *   2. 刷新 UNION ALL 视图，使新月份表纳入视图
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    public void createNextMonthTables() {
        LocalDateTime nextMonth = LocalDateTime.now().plusMonths(1);
        int year = nextMonth.getYear();
        int month = nextMonth.getMonthValue();
        log.info("[分表] 定时任务：为 {}-{} 月创建分表", year, month);
        try {
            createTablesForMonth(year, month);
            updateViews();
            log.info("[分表] {}-{} 月分表创建完成，视图已刷新", year, month);
        } catch (Exception e) {
            log.error("[分表] {}-{} 月分表创建失败", year, month, e);
        }
    }

    /**
     * 每5分钟刷新今日的 health_daily_stats（今天数据仍在持续写入，需要定期更新汇总）
     */
    @Scheduled(fixedDelay = 300_000)
    public void refreshTodayDailyStats() {
        try {
            jdbcTemplate.execute("EXEC sp_refresh_today_daily_stats");
            log.debug("[daily-stats] 今日汇总已刷新");
        } catch (Exception e) {
            log.warn("[daily-stats] 今日汇总刷新失败: {}", e.getMessage());
        }
    }

    /** 调用存储过程创建指定月份的分表（health_record_YYYYMM + warning_record_YYYYMM） */
    private void createTablesForMonth(int year, int month) {
        jdbcTemplate.execute(
                String.format("EXEC sp_create_monthly_tables %d, %d", year, month)
        );
    }

    /** 调用存储过程重建 UNION ALL 视图（扫描所有现有月份表） */
    private void updateViews() {
        jdbcTemplate.execute("EXEC sp_update_monthly_views");
    }
}
