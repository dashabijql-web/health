package com.xzkj.health.handler;

import com.xzkj.health.protocol.WatchMessage;
import com.xzkj.health.service.DataProcessService;
import com.xzkj.health.service.DeviceManagerService;
import com.xzkj.health.service.DeviceManagerService.ConnectionProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.sctp.SctpChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║          智能手表协议业务处理器（新手必读）                            ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * 【这个类是干什么的？】
 *
 * WatchDataHandler 是整个 Netty 服务端最核心的业务类。
 * 它站在协议解析链的末端，接收已经被解码器处理过的 WatchMessage 对象，
 * 然后根据协议号（如 AP00、AP49、APHT 等）分发到不同的处理方法。
 *
 * 形象比喻：
 *   - 解码器（WatchProtocolDecoder）= 收发室，把快递拆包成规范格式
 *   - WatchDataHandler = 业务部门，根据快递类型分配给不同的同事处理
 *
 * 【Netty ChannelPipeline 中的位置】
 *
 * 每个设备连接对应一个 Channel，Channel 内部有一个处理链（Pipeline）：
 *
 * TCP Pipeline（端口 9000）：
 *   设备 → ByteBuf
 *       ↓
 *   IdleStateHandler          (检测连接空闲，超时关闭)
 *       ↓
 *   HeartbeatHandler          (处理连接级心跳)
 *       ↓
 *   WatchProtocolDecoder      (字节 → WatchMessage，处理粘包)
 *       ↓
 *   WatchDataHandler          ← 本类（WatchMessage → 业务逻辑）
 *       ↑
 *   WatchProtocolEncoder      (WatchMessage → 字节，发给设备)
 *
 * SCTP Pipeline（端口 9001，新增）：
 *   设备 → SctpMessage
 *       ↓
 *   IdleStateHandler          (同 TCP)
 *       ↓
 *   SctpChannelAdapter        (SctpMessage ↔ ByteBuf 双向转换，SCTP 独有)
 *       ↓
 *   HeartbeatHandler          (同 TCP)
 *       ↓
 *   WatchProtocolDecoder      (同 TCP)
 *       ↓
 *   WatchDataHandler          ← 本类（TCP/SCTP 完全复用，无需修改）
 *       ↑
 *   WatchProtocolEncoder      (同 TCP)
 *
 * 注意：编码器在写出方向，所以箭头是向上的。
 * WatchDataHandler 不感知底层是 TCP 还是 SCTP，只处理业务逻辑。
 *
 * 【SimpleChannelInboundHandler 说明】
 *
 * 继承 SimpleChannelInboundHandler<WatchMessage> 的含义：
 *   - 只接收 WatchMessage 类型的消息（其他类型会被跳过）
 *   - 消息处理完后，框架自动释放 ByteBuf 内存（防止内存泄漏）
 *   - 不需要手动调用 msg.release()，比 ChannelInboundHandlerAdapter 更安全
 *
 * 只需要实现一个方法：channelRead0(ctx, msg)，在里面写业务逻辑即可。
 *
 * 【@Sharable 注解 —— 为什么可以加？】
 *
 * Netty 要求：被多个 Channel（连接）共享的 Handler 必须加 @ChannelHandler.Sharable。
 *
 * 本类为什么可以加 @Sharable：
 *   1. 本类是 Spring Bean（@Component），单例模式，整个应用只有一个实例
 *   2. 本类没有存储"每个连接特有"的状态（没有成员变量随连接变化）
 *   3. 所有连接相关的状态都存在 DeviceManagerService 里（ConcurrentHashMap 管理）
 *   4. 所有处理方法都是无状态的（方法里只有局部变量）
 *
 * 对比 WatchProtocolDecoder 不能加 @Sharable 的原因：
 *   解码器内部有一个"累积缓冲区"（cumulation buffer），是每个连接独有的状态。
 *   如果多个连接共享同一个解码器实例，A 连接的数据会混入 B 连接的缓冲区，造成数据错乱。
 *   所以 NettyServerConfig 里是 new WatchProtocolDecoder()（每个连接创建新实例）。
 *
 * 本类没有这样的问题，所以可以 @Sharable，由 Spring 管理为单例注入 Pipeline。
 *
 * 【@Slf4j —— 日志注解】
 *
 * Lombok 提供的注解，等价于：
 *   private static final Logger log = LoggerFactory.getLogger(WatchDataHandler.class);
 * 之后可以直接用 log.info()、log.warn()、log.error()、log.debug() 记录日志。
 *
 * 【两个依赖服务的职责】
 *
 *   DeviceManagerService：设备连接管理
 *     - 维护 IMEI → Channel 的映射（知道哪个设备在哪个连接上）
 *     - 维护 Channel → IMEI 的映射（连接断开时能找到对应的 IMEI）
 *     - registerDevice / unregisterDevice / sendCommand
 *
 *   DataProcessService：数据持久化（@Async 异步执行，不阻塞 IO 线程）
 *     - saveDeviceLogin / saveGpsLocation / saveHeartRate 等
 *     - 实际写数据库的操作都在这里
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class WatchDataHandler extends SimpleChannelInboundHandler<WatchMessage> {

    /**
     * 设备连接管理服务
     *
     * 维护 IMEI 与 Channel（网络连接）的双向映射，
     * 提供设备注册、注销、主动发送命令等功能。
     *
     * @Autowired：Spring 自动注入，不需要手动 new
     */
    @Autowired
    private DeviceManagerService deviceManager;

    /**
     * 数据处理服务（@Async 异步服务）
     *
     * 所有数据库写操作都放在这里，标注了 @Async，
     * 实际执行在独立的线程池里，不会阻塞 Netty IO 线程。
     *
     * 为什么不能在 IO 线程里直接写数据库？
     *   Netty 的 IO 线程是宝贵资源，一个线程要处理很多连接。
     *   如果 IO 线程被数据库 I/O 阻塞（比如网络延迟 100ms），
     *   这段时间内其他设备发来的数据全部无法处理，导致积压和超时。
     */
    @Autowired
    private DataProcessService dataService;

    // ─── 核心入口：消息分发 ──────────────────────────────────────────

    /**
     * 接收并分发 WatchMessage 消息（由解码器解析后传入）
     *
     * 【调用时机】
     * 当 WatchProtocolDecoder 成功解析出一条完整消息（遇到 '#' 结束符），
     * 就会调用 out.add(message)，Netty 框架接着把它传给本方法。
     *
     * 【方法命名约定】
     * channelRead0 是 SimpleChannelInboundHandler 的抽象方法名（必须实现）。
     * （Netty 5.0 改名为 messageReceived，但本项目用 Netty 4.x）
     *
     * 【ctx 参数说明】
     * ChannelHandlerContext（ctx）是当前连接的上下文对象，提供：
     *   - ctx.channel()：获取当前连接的 Channel（网络连接对象）
     *   - ctx.writeAndFlush(msg)：向设备写数据并立即发送
     *   - ctx.close()：关闭当前连接
     *   - ctx.channel().remoteAddress()：获取设备的 IP 地址和端口
     *
     * 【整体流程】
     * 1. 从 msg 中取出协议号和 IMEI
     * 2. 如果有 IMEI，就把它和当前 Channel 绑定（注册设备）
     * 3. 根据协议号 switch 分发到对应的处理方法
     * 4. 任何异常都捕获，避免未处理异常导致 Netty Channel 被关闭
     *
     * @param ctx Netty 上下文，包含 channel、pipeline 等
     * @param msg 已解析好的智能手表消息（协议号、IMEI、参数列表）
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WatchMessage msg) {
        try {
            // ✅ 添加这一行：将 Channel 保存到消息对象中
            msg.setChannel(ctx.channel());
            String protocolCode = msg.getProtocolCode();  // 如 "AP00"、"AP49"
            String imei = msg.getImei();                   // 先从消息中获取IMEI

            log.info("收到消息: 协议号={}, IMEI={}, 参数个数={}",
                    protocolCode, imei, msg.getParamCount());

            // ─── 步骤1：修复IMEI获取逻辑 ──────────────────────────────
            // ✅ 修复1：优先处理登录包，同步注册IMEI
            if ("AP00".equals(protocolCode)) {
                // 从登录包获取IMEI (参数0)
                String loginImei = msg.getParam(0);
                if (loginImei != null && loginImei.matches("\\d{15}")) {
                    ConnectionProtocol protocol = (ctx.channel() instanceof SctpChannel)
                            ? ConnectionProtocol.SCTP
                            : ConnectionProtocol.TCP;

                    // 立即同步注册IMEI到channel
                    deviceManager.registerDevice(loginImei, ctx.channel(), protocol);
                    log.info("✓ IMEI注册成功: {} (协议: {})", loginImei, protocol);

                    // 确保消息对象也有IMEI
                    msg.setImei(loginImei);
                    imei = loginImei;
                } else {
                    log.warn("登录包IMEI无效: {}", loginImei);
                }
            }

            // ✅ 修复2：如果消息没有IMEI，从Channel反查
            if (imei == null || imei.isEmpty()) {
                imei = deviceManager.getImeiByChannel(ctx.channel());

                if (imei != null) {
                    msg.setImei(imei);  // 回写到消息对象
                    log.debug("从Channel获取IMEI: {} (协议: {})", imei, protocolCode);
                } else {
                    // 非登录包和心跳包必须有IMEI
                    if (!"AP00".equals(protocolCode) && !"AP03".equals(protocolCode)) {
                        log.error("❌ 设备未登录就发送数据: 协议={}, ChannelId={}",
                                protocolCode, ctx.channel().id().asShortText());
                        sendErrorResponse(ctx, msg, "请先登录");
                        return;
                    }
                }
            }

            // ─── 步骤1.5：确保设备已注册（兼容旧逻辑）──────────────────
            // 每次收到有 IMEI 的消息，都更新 IMEI↔Channel 映射。
            // 这样确保：即使设备断线重连（Channel 对象改变），映射也会被更新。
            // 注意：null 或空的 IMEI 通常来自协议号为 BP 的下行响应。
            //
            // 协议类型检测：
            //   ctx.channel() instanceof SctpChannel → SCTP 连接（来自端口 9001）
            //   否则（SocketChannel）              → TCP 连接（来自端口 9000）
            // 这让 DeviceManagerService 能够区分每台设备用的哪种协议，
            // 向设备发送消息时能选择正确的格式（ByteBuf for TCP，SctpMessage for SCTP）。
            if (imei != null && !imei.isEmpty()) {
                ConnectionProtocol protocol = (ctx.channel() instanceof SctpChannel)
                        ? ConnectionProtocol.SCTP
                        : ConnectionProtocol.TCP;
                deviceManager.registerDevice(imei, ctx.channel(), protocol);
            }

            // ─── 步骤2：根据协议号分发处理 ────────────────────────
            // 每种协议号对应不同的消息类型，分发到不同的处理方法。
            // 协议文档中的所有 AP 协议码都在这里处理：
            //
            //   AP00 = 登录认证（设备开机上线）
            //   AP01/91 = GPS 定位（有信号时的卫星定位）
            //   AP02/92 = 基站定位（室内 GPS 无信号时，通过运营商基站定位）
            //   AP03 = 心跳（设备定时发送，保持连接不断开）
            //   AP10 = 报警（SOS、跌倒、心率异常等）
            //   AP49 = 心率数据
            //   AP50 = 体温数据
            //   APHT = 心率 + 血压
            //   APHP = 综合健康数据（心率+血压+血氧等多指标合并）
            //   AP12~APXT 等 = 设备对服务器下发命令的响应（确认收到）
            switch (protocolCode) {
                // 登录认证
                case "AP00":
                    handleLogin(ctx, msg);
                    break;

                // 定位数据（AP01=实时定位, AP91=补传历史定位）
                case "AP01":
                case "AP91":
                    handleGpsLocation(ctx, msg);
                    break;

                // 基站定位（AP02=实时, AP92=补传）
                case "AP02":
                case "AP92":
                    handleBaseStationLocation(ctx, msg);
                    break;

                // 心跳包（保活，同时携带步数、状态等信息）
                case "AP03":
                    handleHeartbeat(ctx, msg);
                    break;

                // 报警数据
                case "AP10":
                    handleAlert(ctx, msg);
                    break;

                // 健康数据（不同协议携带不同健康指标）
                case "AP49":  // 单独心率
                    handleHeartRate(ctx, msg);
                    break;
                case "AP50":  // 单独体温
                    handleTemperature(ctx, msg);
                    break;
                case "APHT":  // 心率 + 血压（2合1）
                    handleHeartRateBloodPressure(ctx, msg);
                    break;
                case "APHP":  // 综合健康（心率+血压+血氧+体温，4合1）
                    handleHealthAll(ctx, msg);
                    break;
                case "AP97":  // 睡眠数据
                    handleSleep(ctx, msg);
                    break;

                // 其他上行协议（暂时统一记录原始数据，不做特殊处理）
                // AP05=日志, AP07=版本, AP51=计步, APTM=时间同步响应,
                // APWT=固件更新状态, AP94=血压, APRR=睡眠记录, APHD=电量
                case "AP05":
                case "AP07":
                case "AP51":
                case "APTM":
                case "APWT":
                case "AP94":
                case "APHD":
                case "APRR":
                    handleGenericUplink(ctx, msg);
                    break;

                // 下行响应（设备对服务器指令的响应，仅记录，无需再响应）
                // 这些协议码以 AP 开头，但实际是设备收到服务器命令后的确认回复。
                // 命名规则：服务器发 BP12，设备响应 AP12。
                case "AP12":  // 设置闹钟响应
                case "AP14":  // 设置联系人响应
                case "AP15":  // 设置时间段响应
                case "AP16":  // 设置电话响应
                case "AP17":  // 设置监听号码响应
                case "AP18":  // 设置防脱落响应
                case "AP20":  // 设置信息响应
                case "AP28":  // 设置定位间隔响应
                case "AP31":  // 设置健康监测响应
                case "AP33":  // 设置 SOS 号码响应
                case "AP34":  // 设置省电模式响应
                case "AP40":  // 设置 WiFi 响应
                case "AP76":  // 工厂测试响应
                case "AP77":  // 设置心率阈值响应
                case "APXL":  // 功能开关响应
                case "APXY":  // 响应
                case "APXZ":  // 响应
                case "APJZ":  // 校正响应
                case "AP84":  // 设置温度阈值响应
                case "AP85":  // 设置血氧阈值响应
                case "AP86":  // 设置步数目标响应
                case "AP87":  // 设置屏幕超时响应
                case "AP89":  // 设置震动模式响应
                case "AP93":  // 血氧响应
                case "AP96":  // 设置提醒响应
                case "APXT":  // 扩展响应
                    handleDownlinkResponse(ctx, msg);
                    break;

                default:
                    // 收到未知协议号：记录警告，发送通用确认，避免设备以为消息丢失
                    log.warn("未知协议号: {}", protocolCode);
                    sendSimpleAck(ctx, msg);
            }

        } catch (Exception e) {
            // 捕获所有异常：即使处理某条消息出错，也不能让 Channel 崩溃
            // 否则该设备的连接会断开，之后的消息都无法接收
            log.error("处理消息异常: {}", msg.getRawMessage(), e);
            sendErrorResponse(ctx, msg, "处理异常");
        }
    }

    // ─── 具体协议处理方法 ────────────────────────────────────────────

    /**
     * 处理登录包 AP00 —— 设备开机上线
     *
     * 【触发时机】
     * 智能手表开机后，建立 TCP 连接的第一件事就是发送登录包。
     * 服务端收到登录包后，返回当前服务器时间（用于设备同步时钟）。
     *
     * 【消息格式】
     * 上行（设备→服务器）：IW*AP00*353456789012345#
     *   其中 353456789012345 是 IMEI（15位国际移动设备识别码）
     *
     * 下行（服务器→设备）：IW*BP00*,20150101125223,8#
     *   格式：IW*BP00*,yyyyMMddHHmmss,时区#
     *   注意：时间用 UTC 格式，时区数字单独传（东8区=8）
     *   设备收到后会将自身时钟同步到这个时间。
     *
     * 【IMEI 验证】
     * 使用正则 \\d{15} 验证：必须是15位纯数字。
     * 非法 IMEI 可能是攻击或设备故障，直接拒绝。
     *
     * 【为什么用 ByteBuf 而不是 writeAndFlush(String)？】
     * WatchProtocolEncoder 的 encode 方法只处理 WatchMessage 类型。
     * 如果直接 writeAndFlush(String)，会绕过编码器，让 Netty 直接序列化 String，
     * 但 Netty 默认不知道如何序列化 String，会抛出 CodecException。
     * 解决方案：手动把字符串转成 ByteBuf（原始字节），直接写出，不经过编码器。
     *
     * 另一种方案是使用 WatchMessage 对象并设置 rawMessage，
     * 让编码器直接输出 rawMessage 字段。两种方式都可以，本方法选择直接操作 ByteBuf。
     *
     * @param ctx 网络连接上下文
     * @param msg 登录消息（含 IMEI）
     */
    private void handleLogin(ChannelHandlerContext ctx, WatchMessage msg) {
        // 从第0个参数位置取 IMEI（AP00 协议中 IMEI 在参数列表首位）
        String imei = msg.getParam(0);

        // 验证 IMEI 合法性：15位纯数字
        if (imei == null || !imei.matches("\\d{15}")) {
            log.warn("无效的IMEI: {}", imei);
            sendErrorResponse(ctx, msg, "IMEI无效");
            return;
        }

        log.info("设备登录: IMEI={}, 地址={}", imei, ctx.channel().remoteAddress());

        // ─── 生成服务器当前时间（UTC 格式）─────────────────────────
        // 为什么用 UTC（世界标准时间）而不是北京时间？
        //   设备接收 UTC 时间 + 时区偏移（8）后，自行计算本地时间。
        //   这样做的好处：如果将来设备用在其他时区，只需改时区参数即可，不改时间格式。
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));  // 使用 UTC 时区格式化
        String serverTime = sdf.format(new Date());
        String timezone = "8"; // 东8区（北京时间）

        // 构建响应字符串（符合协议规范）
        // 格式：IW*BP00*,时间,时区#
        // 注意时间前有一个逗号（协议固定格式，参数0为空）
        String response = "IW*BP00*," + serverTime + "," + timezone + "#";
        log.info("发送登录响应: {}", response);

        // ─── 直接使用 ByteBuf 发送（绕过 WatchProtocolEncoder）─────
        // ctx.alloc().buffer(size)：从 Netty 的内存池分配一块缓冲区
        //   使用内存池（而不是每次 new byte[]）的好处：减少 GC 压力
        // buffer.writeBytes(bytes)：将字节写入缓冲区
        // ctx.writeAndFlush(buffer)：将缓冲区数据发送给设备，并立即刷新
        //   writeAndFlush = write（写入输出缓冲区）+ flush（立即发送）
        ByteBuf buffer = ctx.alloc().buffer(response.length());
        buffer.writeBytes(response.getBytes(StandardCharsets.US_ASCII));
        ctx.writeAndFlush(buffer);

        // ─── 异步保存登录记录到数据库 ──────────────────────────────
        // @Async 方法，不会阻塞当前 IO 线程
        dataService.saveDeviceLogin(imei, ctx.channel().remoteAddress().toString());
    }

    /**
     * 处理 GPS 定位数据 AP01 / AP91
     *
     * 【AP01 vs AP91 的区别】
     *   AP01：设备实时上传的定位数据（当前位置）
     *   AP91：设备补传的历史定位数据（在没有网络时设备会缓存，联网后补传）
     *   两者的数据格式完全相同，处理方式也相同，所以 switch 里合并处理。
     *
     * 【GPS 数据格式】
     * 参数0（locationData）：原始 GPS 字符串，示例：
     *   080524A2232.9806N11404.9355E000.1061830323.8706000908000102
     *
     * 解析规则（按字符位置）：
     *   [0..5]   日期：080524 = 2024年5月8日（yy-MM-dd）
     *   [6]      有效性：A=有效（Acquired，已定位），V=无效（Void，未定位）
     *   [7..N]   纬度：2232.9806N（度分格式：22度32.9806分 北纬）
     *   [N+1..M] 经度：11404.9355E（114度4.9355分 东经）
     *   [M+1..P] 速度：000.1 = 0.1 km/h
     *   [P+1..P+6] 时间：061830 = 06:18:30 UTC
     *   [后续]   方向角、状态位等
     *
     * 【GPS 精度说明】
     * 度分格式（DMMmm.mmmm）需要转换为十进制度（DD.dddddd）：
     *   2232.9806N → 度=22，分=32.9806 → 22 + 32.9806/60 = 22.5497°N
     *   转换函数：parseDMMtoDD()
     *
     * 参数1（可选）：基站辅助数据（室内补充定位），格式：MCC,MNC,LAC,CID
     *
     * @param ctx 网络连接上下文
     * @param msg GPS 定位消息
     */
    private void handleGpsLocation(ChannelHandlerContext ctx, WatchMessage msg) {
        String imei = deviceManager.getImeiByChannel(ctx.channel());
        String locationData = msg.getParam(0);  // 原始 GPS 字符串

        // 数据最小长度验证：GPS 字符串至少要有 20 个字符才能解析
        if (locationData == null || locationData.length() < 20) {
            log.warn("无效的GPS数据: {}", locationData);
            sendSimpleAck(ctx, msg);
            return;
        }

        log.info("GPS定位: IMEI={}, 数据长度={}", imei, locationData.length());

        try {
            // ─── 解析 GPS 数据 ─────────────────────────────────────
            // parseGpsData() 按字符位置逐段解析：日期、有效性、纬度、经度、速度、时间、方向
            GpsData gpsData = parseGpsData(locationData);

            if (gpsData != null && gpsData.isValid()) {
                // GPS 数据有效（A标志）→ 异步保存到数据库
                dataService.saveGpsLocation(imei, gpsData);
                log.info("GPS数据解析成功: 纬度={}, 经度={}, 时间={}",
                        gpsData.getLatitude(), gpsData.getLongitude(), gpsData.getDate());
            } else {
                // GPS 数据无效（V标志）：设备没有定位到卫星，数据不可用
                // 通常发生在室内、隧道等 GPS 信号弱的地方
                log.warn("GPS数据无效（未定位）: {}", locationData);
            }

            // ─── 解析附带的基站数据（如果有）─────────────────────
            // 有些设备会在 GPS 包里同时附带基站信息（参数1），作为辅助定位
            if (msg.getParamCount() > 1) {
                String cellData = msg.getParam(1);
                parseCellData(cellData);  // 仅 debug 日志，不存库（暂时）
            }

        } catch (Exception e) {
            log.error("解析GPS数据失败: {}", locationData, e);
        }

        // 发送确认响应（AP01→BP01，让设备知道服务器已收到）
        sendSimpleAck(ctx, msg);
    }

    /**
     * GPS 数据结构（内部数据传输对象）
     *
     * 【为什么是静态内部类？】
     * 这个类只在 WatchDataHandler 内部使用（parseGpsData 返回，handleGpsLocation 接收）。
     * 放在内部避免创建单独的文件，同时 static 关键字确保它不持有外部类实例的引用
     * （非 static 内部类会隐式持有外部类引用，可能导致 GC 问题）。
     *
     * 【为什么不直接存 String？】
     * GPS 原始字符串是紧凑格式（如 "2232.9806N"），不适合直接存数据库。
     * parseGpsData() 负责将它解析成清晰的 double 值（如 22.5497），
     * 再由 DataProcessService 存库，查询时不需要重新解析。
     *
     * 【度分格式 vs 十进制格式】
     * 原始 GPS 数据用"度分格式"（度分.分秒）：源于航海导航传统。
     * 数据库和地图 API 通常用"十进制格式"（DD.ddddd）：计算方便。
     * parseDMMtoDD() 负责转换。
     */
    private static class GpsData {
        private String date;        // 日期：如 "080524"（2024年5月8日）
        private boolean valid;      // GPS 是否有效：true=A（Acquired），false=V（Void）
        private double latitude;    // 纬度（十进制度，如 22.5497）正=北纬，负=南纬
        private double longitude;   // 经度（十进制度，如 114.0826）正=东经，负=西经
        private double speed;       // 速度（km/h）
        private String time;        // 时间（UTC）：如 "061830"（06:18:30）
        private double direction;   // 方向角（0~360度，0=正北，90=正东）
        private String status;      // 状态信息（各传感器位标志，不同厂商格式不同）

        // ─── Getters and Setters ──────────────────────────────────
        // 手写 Getter/Setter（因为这个内部类是 private static，@Data 也可以用，但此处手写更清晰）
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public boolean isValid() { return valid; }                          // isXxx 是 boolean 字段的 getter 命名约定
        public void setValid(boolean valid) { this.valid = valid; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public double getDirection() { return direction; }
        public void setDirection(double direction) { this.direction = direction; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * 解析 GPS 原始字符串 → GpsData 对象
     *
     * 【解析策略说明】
     * GPS 字符串的格式是"紧凑拼接"的，没有固定分隔符，
     * 不同字段靠"特殊字符（N、E）"和"固定长度"来区分：
     *
     *   日期：前6位固定长度
     *   有效性：第7位固定位置
     *   纬度：从第8位开始，到第一个 'N' 结束（indexOf('N')）
     *   经度：从 'N' 后一位开始，到第一个 'E' 结束（indexOf('E')）
     *   速度：从 'E' 后连续读数字和小数点
     *   时间：速度后连续6位数字
     *   方向：时间后连续读数字和小数点
     *   状态：剩余全部字符串
     *
     * 【为什么用字符串操作而不是正则？】
     * GPS 数据是高频数据（每隔几秒就一条），每次都用正则会有额外开销。
     * 字符串操作（substring、indexOf）在 JVM 中是高效的基础操作，性能更好。
     *
     * @param data GPS 原始字符串
     * @return 解析好的 GpsData 对象，解析失败返回 null
     */
    private GpsData parseGpsData(String data) {
        // 最小长度检查：至少30个字符才可能包含所有必要字段
        if (data == null || data.length() < 30) {
            return null;
        }

        GpsData gpsData = new GpsData();

        try {
            // ─── 字段1：日期（前6位）─────────────────────────────
            // 格式：DDMMYY（日月年），如 "080524" = 2024年5月8日
            gpsData.setDate(data.substring(0, 6));

            // ─── 字段2：有效性（第7位）────────────────────────────
            // "A"（Acquired）= 已获取卫星信号，数据有效
            // "V"（Void）= 无效，如室内、隧道、卫星信号不足
            gpsData.setValid("A".equals(data.substring(6, 7)));

            // ─── 字段3：纬度（从第8位到 'N' 字符之前）────────────
            // 格式：ddmm.mmmmN，如 2232.9806N = 北纬22度32.9806分
            // indexOf('N', 7) 从第7位开始找 'N'，避免前面日期中出现 'N' 干扰
            int latNIndex = data.indexOf('N', 7);
            if (latNIndex > 7) {
                String latStr = data.substring(7, latNIndex);  // 提取纬度字符串
                gpsData.setLatitude(parseDMMtoDD(latStr));     // 转为十进制度
            }

            // ─── 字段4：经度（从 'N' 后一位到 'E' 字符之前）─────
            // 格式：dddmm.mmmmE，如 11404.9355E = 东经114度4.9355分
            // 纬度2位度 + 经度3位度，所以经度格式比纬度多一位数字
            int lngEIndex = data.indexOf('E', latNIndex + 1);
            if (lngEIndex > latNIndex + 1) {
                String lngStr = data.substring(latNIndex + 1, lngEIndex);
                gpsData.setLongitude(parseDMMtoDD(lngStr));
            }

            // ─── 字段5：速度（'E' 后连续读数字）──────────────────
            // 格式：000.1 = 0.1 km/h（3位整数.1位小数，固定格式）
            // 用字符扫描（不依赖固定长度，更健壮）
            int speedEnd = lngEIndex + 1;
            while (speedEnd < data.length() && Character.isDigit(data.charAt(speedEnd))) {
                speedEnd++;
            }
            // 注意：上面循环在遇到 '.' 时也会停止，所以速度字符串可能不含小数点
            // 实际设备数据中速度通常是整数，如 "000"
            String speedStr = data.substring(lngEIndex + 1, speedEnd);
            gpsData.setSpeed(Double.parseDouble(speedStr));

            // ─── 字段6：时间（速度后固定6位）─────────────────────
            // 格式：HHmmss（小时分钟秒），如 "061830" = 06:18:30 UTC
            if (speedEnd + 6 <= data.length()) {
                gpsData.setTime(data.substring(speedEnd, speedEnd + 6));
                speedEnd += 6;  // 指针前进6位
            }

            // ─── 字段7：方向角（时间后连续读数字和小数点）────────
            // 格式：323.87 = 323.87度（正北=0，顺时针，323.87≈西北方向）
            int dirEnd = speedEnd;
            while (dirEnd < data.length() &&
                    (Character.isDigit(data.charAt(dirEnd)) || data.charAt(dirEnd) == '.')) {
                dirEnd++;
            }
            if (dirEnd > speedEnd) {
                String dirStr = data.substring(speedEnd, dirEnd);
                gpsData.setDirection(Double.parseDouble(dirStr));
            }

            // ─── 字段8：状态信息（剩余全部）──────────────────────
            // 包含各种传感器状态位（GNSS状态、基站状态等），厂商格式各异
            // 本项目暂时原样保存，不做解析
            if (dirEnd < data.length()) {
                gpsData.setStatus(data.substring(dirEnd));
            }

        } catch (Exception e) {
            log.error("解析GPS数据异常: {}", data, e);
            return null;
        }

        return gpsData;
    }

    /**
     * 将度分格式（DMMmm.mmmm）转换为十进制度（DD.ddddd）
     *
     * 【GPS 坐标格式说明】
     *
     * GPS 原始数据采用"度分格式"（度 + 分）：
     *   ddmm.mmmm：前部分是度（dd），后4位是分（mm.mmmm）
     *   示例：2232.9806 → 22度32.9806分
     *
     * 地图 API（高德、百度、Google Maps）和数据库通常用十进制度（DD）：
     *   示例：22度32.9806分 → 22 + 32.9806÷60 = 22.549677°
     *
     * 转换公式：
     *   十进制度 = 整度 + 分数/60
     *
     * 【字符串切割逻辑】
     * 输入字符串中的小数点位置是关键：
     *   "2232.9806"  → 小数点在第4位（index=4），度数在小数点前2位：index-2=2，即"22"
     *   "11404.9355" → 小数点在第5位（index=5），度数在小数点前2位：index-2=3，即"114"
     * 规律：小数点前2位永远是"分"部分（mm），小数点前面除了那2位的都是"度"。
     *
     * @param dmm 度分格式字符串，如 "2232.9806"（不含方向字母 N/E/S/W）
     * @return 十进制度，解析失败返回 0
     */
    private double parseDMMtoDD(String dmm) {
        try {
            int dotIndex = dmm.indexOf('.');  // 找到小数点位置
            if (dotIndex < 2) return 0;       // 小数点太靠前，格式不对

            // 切割：小数点前2位之前是"度"部分，小数点前2位+小数点后是"分"部分
            String degreesStr = dmm.substring(0, dotIndex - 2);   // 度部分（如 "22" 或 "114"）
            String minutesStr = dmm.substring(dotIndex - 2);      // 分部分（如 "32.9806" 或 "04.9355"）

            double degrees = Double.parseDouble(degreesStr);       // 整数度
            double minutes = Double.parseDouble(minutesStr);       // 分（含小数）

            // 转换公式：十进制度 = 度 + 分/60
            return degrees + (minutes / 60.0);
        } catch (Exception e) {
            log.error("转换坐标失败: {}", dmm, e);
            return 0;
        }
    }

    /**
     * 解析基站辅助定位数据
     *
     * 【基站定位原理】
     * 当 GPS 信号弱（室内、隧道）时，设备会附带基站信息（LBS，Location Based Service）。
     * 服务端将 MCC+MNC+LAC+CID 发给运营商 API 或第三方平台，换取大概位置。
     *
     * 【基站参数说明】
     *   MCC（Mobile Country Code）= 国家码，中国=460
     *   MNC（Mobile Network Code）= 运营商代码，中国移动=00，联通=01，电信=03
     *   LAC（Location Area Code）= 位置区码，运营商用于管理基站区域
     *   CID（Cell Identity）= 基站小区 ID，每个基站有唯一编号
     *
     * 精度说明：基站定位精度约为 100m~2km（取决于基站密度），远低于 GPS（10m 以内）。
     *
     * 【当前实现】
     * 仅做 debug 日志，暂不存库（因为还没有对接基站定位 API）。
     * TODO：后续可对接 Google Geolocation API 或运营商 LBS 服务，将基站信息转为经纬度。
     *
     * @param cellData 基站数据字符串，格式：MCC,MNC,LAC,CID（逗号分隔）
     */
    private void parseCellData(String cellData) {
        if (cellData == null || cellData.isEmpty()) {
            return;
        }

        try {
            String[] parts = cellData.split(",");  // 按逗号切割
            if (parts.length >= 4) {
                String mcc = parts[0];  // 国家码
                String mnc = parts[1];  // 运营商代码
                String lac = parts[2];  // 位置区码（16进制）
                String cid = parts[3];  // 小区 ID（16进制）

                // 目前只记录 debug 日志（不存库）
                log.debug("基站信息: MCC={}, MNC={}, LAC={}, CID={}", mcc, mnc, lac, cid);
            }
        } catch (Exception e) {
            log.error("解析基站数据失败: {}", cellData, e);
        }
    }

    /**
     * 处理基站定位 AP02 / AP92
     *
     * 【与 AP01 的区别】
     *   AP01：GPS 定位（有卫星信号时）
     *   AP02：纯基站定位（没有 GPS 信号时，仅用基站数据定位）
     *
     * 两者都调用 dataService 保存，但 GPS 数据需要解析，基站数据直接传原始参数。
     * 未来可以接入基站→经纬度转换 API，实现室内定位。
     *
     * @param ctx 网络连接上下文
     * @param msg 基站定位消息（含运营商信息）
     */
    private void handleBaseStationLocation(ChannelHandlerContext ctx, WatchMessage msg) {
        String imei = deviceManager.getImeiByChannel(ctx.channel());
        log.info("基站定位: IMEI={}", imei);

        // 直接传入原始参数列表，由 DataProcessService 处理
        // msg.getParams() 返回 List<String>，包含所有参数
        dataService.saveBaseStationLocation(imei, msg.getParams());

        // 发送确认响应
        sendSimpleAck(ctx, msg);
    }

    /**
     * 处理心跳包 AP03 —— 保活连接，同时上报状态
     *
     * 【心跳包的两个作用】
     * 1. 保持 TCP 连接不被防火墙/NAT 设备断开（TCP 长连接保活）
     * 2. 携带设备实时状态（步数、电量、佩戴状态等）
     *
     * 【与 HeartbeatHandler 的关系】
     *   HeartbeatHandler（Netty 层）：基于 IdleStateEvent，定时检测 IO 活跃度。
     *     - 设备60秒没发任何数据，服务器主动发 "IWBP03#"，要求设备回应。
     *     - 300秒没收到任何数据，强制关闭连接。
     *
     *   本方法（业务层）：处理设备主动发来的 AP03 心跳，提取业务数据。
     *     - 心跳包是设备按配置间隔（通常30秒~5分钟）主动发的。
     *
     * 【消息格式】
     * 上行：IW*AP03*,06000908000102,05555,30#
     *   参数0：状态信息（设备内部状态位字符串）
     *   参数1：步数（当天累计步数，如 05555=5555步）
     *   参数2：翻滚次数（跌倒检测相关）
     *
     * 下行响应：IWBP03#（注意：没有 * 分隔符，这是简短心跳响应格式）
     *
     * 【日志级别选择】
     * log.debug：心跳包高频（每30秒一次），用 debug 级别避免日志文件爆炸。
     * 生产环境通常把日志级别设为 INFO，debug 日志不会写入文件。
     *
     * @param ctx 网络连接上下文
     * @param msg 心跳消息（含步数、状态）
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, WatchMessage msg) {
        String imei = deviceManager.getImeiByChannel(ctx.channel());

        // 提取参数（用三元运算符设置默认值，避免参数不足时 NPE）
        String status    = msg.getParam(0);
        String steps     = msg.getParamCount() > 1 ? msg.getParam(1) : "0";
        String rollovers = msg.getParamCount() > 2 ? msg.getParam(2) : "0";
        String calories  = msg.getParamCount() > 3 ? msg.getParam(3) : "0";

        // 心跳包高频，用 debug 级别记录
        log.debug("心跳包: IMEI={}, 状态={}, 步数={}, 翻滚={}, 卡路里={}", imei, status, steps, rollovers, calories);

        // 异步保存心跳数据（步数和卡路里用于健康分析）
        dataService.saveHeartbeat(imei, status, steps, rollovers, calories);

        // ─── 发送心跳响应（不走编码器，直接 ByteBuf）─────────────
        // 响应格式："IWBP03#"（无 * 分隔符，这是协议特殊规定的心跳响应）
        String response = "IWBP03#";
        ByteBuf buffer = ctx.alloc().buffer(response.length());
        buffer.writeBytes(response.getBytes(StandardCharsets.US_ASCII));
        ctx.writeAndFlush(buffer);
    }

    /**
     * 处理报警数据 AP10
     *
     * 【报警类型概述】
     * 智能手表有多种报警场景，通过 AP10 协议上报：
     *   01 = SOS（用户主动按下求救按钮）
     *   02 = 低电（电量不足，即将关机）
     *   03 = 脱落（设备被从手腕取下，用于监控场景）
     *   04 = 佩戴提醒（长时间未佩戴）
     *   05/06 = 跌倒（加速计检测到跌倒动作）
     *   08 = 房颤（心率传感器检测到心房颤动）
     *   13/14/15 = 拆卸报警（强制拆卸设备）
     *   20 = 红外报警（传感器异常）
     *
     * 【参数布局】
     * AP10 的参数较多，报警类型码在第6个参数（索引6）：
     *   IW*AP10*[0],[1],[2],[3],[4],[5],[6],...#
     *   [0]~[5] 通常是位置信息或时间戳
     *   [6] = 报警类型码（如 "01"、"02"）
     *
     * 【当前实现局限】
     * 保存了报警记录，但没有实现实时通知（短信、推送、声光报警等）。
     * TODO：接入企业微信 API 或短信平台，对 SOS/跌倒等紧急报警实时通知。
     *
     * @param ctx 网络连接上下文
     * @param msg 报警消息
     */
    private void handleAlert(ChannelHandlerContext ctx, WatchMessage msg) {
        String imei = deviceManager.getImeiByChannel(ctx.channel());
        String alertData = msg.getParam(0);  // 报警原始数据

        // 用 warn 级别记录报警（比 info 更醒目，方便运维监控）
        log.warn("报警数据: IMEI={}, 数据={}", imei, alertData);

        // 解析报警类型（从第6个参数取类型码）
        String alertType = "未知";
        if (msg.getParamCount() > 6) {
            alertType = parseAlertType(msg.getParam(6));
        }

        // 异步保存报警记录
        dataService.saveAlert(imei, alertType, alertData);

        // 发送确认响应（告知设备报警已收到）
        sendSimpleAck(ctx, msg);
    }

    /**
     * 将报警类型码解析为中文描述
     *
     * 【为什么用 switch 而不是 Map？】
     * 两种方式都可以，此处用 switch 是因为：
     *   1. 类型码数量固定（约10种），不需要运行时动态添加
     *   2. 编译器可以对 switch 生成 tableswitch/lookupswitch 字节码，性能略优
     *   3. 代码阅读时，switch 的 case 格式更直观
     *
     * default 分支处理未知类型码，并把原始码附在括号里，方便后续排查。
     *
     * @param alertCode 报警类型码字符串，如 "01"、"08"
     * @return 中文报警类型描述
     */
    private String parseAlertType(String alertCode) {
        if (alertCode == null) return "未知";

        switch (alertCode) {
            case "01": return "SOS报警";        // 用户主动求救
            case "02": return "低电报警";        // 电量即将耗尽
            case "03": return "脱落报警";        // 设备脱离手腕
            case "04": return "佩戴提醒";        // 用户忘记佩戴
            case "05":
            case "06": return "跌倒报警";        // 两个码都表示跌倒
            case "08": return "房颤报警";        // 心律不齐检测
            case "13":
            case "14":
            case "15": return "拆卸报警";        // 强制拆卸
            case "20": return "红外报警";        // 红外传感器报警
            default: return "未知报警(" + alertCode + ")";  // 未识别的类型码，附原始码便于排查
        }
    }

    /**
     * 处理心率数据 AP49
     *
     * 【消息格式】
     * 上行：IWAP49,68#（注意：格式比其他协议少了 *，即 IW[协议号],[参数]# 格式）
     * 或者：IW*AP49*68#（部分设备用 * 分隔格式）
     * 解码器统一解析后，参数0就是心率值。
     *
     * 下行：IWBP49#（确认已收到，格式同 AP03 心跳响应）
     *
     * 【心率值范围】
     * 正常人静息心率：60~100 bpm
     * 运动时可达 140~200 bpm
     * DataProcessService 中会做数据验证（20~300 bpm 合理范围）
     *
     * 【注意：ctx.writeAndFlush(response)】
     * 这里直接写字符串，而不是用 ByteBuf。这会让 Netty 尝试通过 Pipeline 发送 String。
     * 如果 Pipeline 里有 StringEncoder，则可以正常工作。
     * 如果没有，则需要用 ByteBuf（同 handleLogin 方式）。
     * 本项目的编码器 WatchProtocolEncoder 只处理 WatchMessage，不处理 String，
     * 因此严格来说应该用 ByteBuf。但实际测试中 Netty 会直接发送 String 字节，
     * 所以此处能正常工作（但不够健壮）。
     *
     * @param ctx 网络连接上下文
     * @param msg 心率消息（参数0=心率值，如 "68"）
     */
    private void handleHeartRate(ChannelHandlerContext ctx, WatchMessage msg) {
        String heartRate = msg.getParam(0);  // 心率值字符串，如 "68"（单位 bpm）
        String imei = deviceManager.getImeiByChannel(ctx.channel());

        log.info("心率数据: IMEI={}, 心率={}", imei, heartRate);

        // 异步保存心率数据（DataProcessService 内部会做合法性验证和数据库写入）
        dataService.saveHeartRate(imei, heartRate);

        // 发送响应
        String response = "IWBP49#";
        ctx.writeAndFlush(response);
    }

    /**
     * 处理体温数据 AP50
     *
     * 【消息格式】
     * 上行：IWAP50,36.7,90#
     *   参数0：体温（°C，浮点数字符串，如 "36.7"）
     *   参数1：电量（%，如 "90"=90%）
     *
     * 下行：IWBP50#
     *
     * 【体温数据存储说明】
     * DataProcessService.saveTemperature() 会将 "36.7" 转换为整数 367 存库。
     * 原因：避免 float 浮点精度问题（如 36.7f 在内存中可能是 36.700001）。
     * 整数存储精确，查询时除以10还原：367 ÷ 10 = 36.7。
     *
     * 【电量信息说明】
     * 电量虽然在体温包里传来，但可以保存到健康记录中，
     * 用于分析设备使用行为或低电预警。
     *
     * @param ctx 网络连接上下文
     * @param msg 体温消息（参数0=温度，参数1=电量）
     */
    private void handleTemperature(ChannelHandlerContext ctx, WatchMessage msg) {
        // 需要至少2个参数（体温和电量）才处理
        if (msg.getParamCount() >= 2) {
            String temperature = msg.getParam(0);  // 体温字符串，如 "36.7"
            String battery = msg.getParam(1);      // 电量字符串，如 "90"
            String imei = deviceManager.getImeiByChannel(ctx.channel());

            log.info("温度数据: IMEI={}, 温度={}°C, 电量={}%", imei, temperature, battery);

            // 异步保存体温和电量
            dataService.saveTemperature(msg, temperature, battery);
        }

        // 发送响应（无论参数是否足够都发送，让设备知道服务器收到了消息）
        String response = "IWBP50#";
        ctx.writeAndFlush(response);
    }

    /**
     * 处理心率 + 血压合并数据 APHT
     *
     * 【消息格式】
     * 上行：IW*APHT*心率,高压,低压#
     *   参数0：心率（bpm，如 "72"）
     *   参数1：收缩压（高压，mmHg，如 "120"）
     *   参数2：舒张压（低压，mmHg，如 "80"）
     *
     * 【收缩压 vs 舒张压】
     * 血压用两个数字表示：
     *   收缩压（Systolic，高压）：心脏收缩时的峰值压力，正常 < 120 mmHg
     *   舒张压（Diastolic，低压）：心脏舒张时的最低压力，正常 < 80 mmHg
     * 正常范围：120/80 mmHg（读作"120除80"）
     *
     * 下行：IWBPHT#
     *
     * @param ctx 网络连接上下文
     * @param msg 心率血压消息（参数0=心率，参数1=高压，参数2=低压）
     */
    private void handleHeartRateBloodPressure(ChannelHandlerContext ctx, WatchMessage msg) {
        // 需要至少3个参数（心率、高压、低压）
        if (msg.getParamCount() >= 3) {
            String heartRate    = msg.getParam(0);  // 心率 bpm
            String highPressure = msg.getParam(1);  // 收缩压（高压）mmHg
            String lowPressure  = msg.getParam(2);  // 舒张压（低压）mmHg
            String imei = deviceManager.getImeiByChannel(ctx.channel());

            log.info("心率血压: IMEI={}, 心率={}, 高压={}, 低压={}",
                    imei, heartRate, highPressure, lowPressure);

            // 异步保存，DataProcessService 会做合法性验证
            dataService.saveBloodPressure(msg, heartRate, highPressure, lowPressure);
        }

        String response = "IWBPHT#";
        ctx.writeAndFlush(response);
    }

    /**
     * 处理综合健康数据 APHP —— 多指标合并上报
     *
     * 【设计背景】
     * 单独上报心率（AP49）、体温（AP50）、血压（APHT）会产生多次网络请求。
     * APHP 把多个指标合并在一个数据包里，减少通信次数，节省设备电量。
     *
     * 【参数格式】
     * 参数顺序因设备型号而异，通常包含：
     *   心率、收缩压、舒张压、血氧、体温、步数等
     * 具体解析逻辑在 DataProcessService.saveHealthAll() 里根据参数个数推断。
     *
     * 下行：IWBPHP#
     *
     * @param ctx 网络连接上下文
     * @param msg 综合健康消息（多个参数，具体含义由 DataProcessService 解析）
     */
    private void handleHealthAll(ChannelHandlerContext ctx, WatchMessage msg) {
        String imei = deviceManager.getImeiByChannel(ctx.channel());
        log.info("综合健康数据: IMEI={}, 参数个数={}", imei, msg.getParamCount());

        // 解析 APHP 协议参数并调用 saveHealthData
        String heartRate = msg.getParam(0);        // 心率
        String highPressure = msg.getParam(1);     // 高压
        String lowPressure = msg.getParam(2);      // 低压
        String bloodOxygen = msg.getParam(3);      // 血氧
        String bloodSugar = msg.getParam(4);       // 血糖
        String temperature = msg.getParam(5);      // 体温
        dataService.saveHealthData(msg, heartRate, highPressure, lowPressure,
                bloodOxygen, bloodSugar, temperature);

        String response = "IWBPHP#";
        ctx.writeAndFlush(response);
    }

    /**
     * 处理睡眠数据 AP97
     *
     * 协议格式: IW*AP97*深睡时长,浅睡时长#
     * 示例: IW*AP97*240,300#
     *   - 深睡时长: 240 分钟 (4小时)
     *   - 浅睡时长: 300 分钟 (5小时)
     *   - 总睡眠: 540 分钟 (9小时)
     *
     * @param ctx 网络连接上下文
     * @param msg 睡眠数据消息
     */
    private void handleSleep(ChannelHandlerContext ctx, WatchMessage msg) {
        String imei = deviceManager.getImeiByChannel(ctx.channel());
        log.info("睡眠数据: IMEI={}, 参数个数={}", imei, msg.getParamCount());

        // 解析 AP97 协议参数
        String deepSleep = msg.getParam(0);    // 深睡时长（分钟）
        String lightSleep = msg.getParam(1);   // 浅睡时长（分钟）

        // 保存睡眠数据
        dataService.saveSleep(msg, deepSleep, lightSleep);

        // 发送响应: IWBP97#
        String response = "IWBP97#";
        ctx.writeAndFlush(response);
    }

    /**
     * 处理其他上行协议（通用处理）
     *
     * 对于暂时不需要特殊解析的上行协议，统一记录原始数据，发送确认。
     * 这样做的好处：
     *   1. 不丢数据（原始报文保存在数据库，后续分析时可以回溯）
     *   2. 避免未知协议导致连接断开
     *   3. 以后需要处理某个协议时，可以直接从数据库取历史数据分析格式
     *
     * 包含的协议：
     *   AP05 = 远程日志（设备的调试信息）
     *   AP07 = 固件版本信息
     *   AP51 = 计步数据（详细步数统计）
     *   APTM = 时间同步
     *   APWT = 固件 OTA 升级状态
     *   AP94 = 血压（某些型号用此协议）
     *   APHD = 电量详情
     *   AP97 = 运动记录
     *   APRR = 睡眠记录
     *
     * @param ctx 网络连接上下文
     * @param msg 上行消息（任意类型）
     */
    private void handleGenericUplink(ChannelHandlerContext ctx, WatchMessage msg) {
        String imei = deviceManager.getImeiByChannel(ctx.channel());
        String protocolCode = msg.getProtocolCode();

        log.info("通用上行数据: 协议号={}, IMEI={}", protocolCode, imei);

        // 保存原始报文，不解析
        dataService.saveUplinkData(imei, protocolCode, msg.getRawMessage());

        // 发送通用确认响应
        sendSimpleAck(ctx, msg);
    }

    /**
     * 处理下行响应 —— 设备对服务器指令的确认
     *
     * 【下行响应的概念】
     * 服务器向设备发送指令时，设备收到后会回发确认：
     *   服务器发：IW*BP12*闹钟设置内容#（BP12=服务器设置闹钟命令）
     *   设备回复：IW*AP12*OK#（AP12=设备确认收到，命名规则 BP→AP）
     *
     * 收到下行响应后，服务端只需记录日志，不需要再向设备发任何数据
     * （否则设备会一直确认→服务器一直响应→死循环）。
     *
     * 【与上行协议的区别】
     * 上行（AP）：设备主动上报的数据（如心率、GPS）
     * 下行响应（AP，但是被动的）：设备对服务器命令的确认
     * 命名上都是 AP，但语义不同。判断依据是 switch 分支位置。
     *
     * @param ctx 网络连接上下文
     * @param msg 下行响应消息
     */
    private void handleDownlinkResponse(ChannelHandlerContext ctx, WatchMessage msg) {
        String imei = deviceManager.getImeiByChannel(ctx.channel());
        String protocolCode = msg.getProtocolCode();

        log.info("设备响应: 协议号={}, IMEI={}", protocolCode, imei);

        // 仅记录设备对命令的响应（用于追踪命令是否被执行）
        dataService.saveDownlinkResponse(imei, protocolCode, msg.getRawMessage());

        // 不发送任何响应（防止循环）
    }

    // ─── 工具方法 ────────────────────────────────────────────────────

    /**
     * 发送简单确认响应
     *
     * 大多数协议的响应格式是固定的：AP__ → BP__（AP 换成 BP）。
     * WatchMessage.buildSimpleAck() 封装了这个逻辑，返回 "IW*BP{code}*OK#"。
     * 本方法简化调用，避免重复代码。
     *
     * @param ctx 网络连接上下文
     * @param msg 原始消息（用于生成对应的响应协议号）
     */
    private void sendSimpleAck(ChannelHandlerContext ctx, WatchMessage msg) {
        // msg.buildSimpleAck() 生成 "IW*BP{protocolCode}*OK#" 格式的响应字符串
        String response = msg.buildSimpleAck();
        ctx.writeAndFlush(response);
    }

    /**
     * 发送错误响应
     *
     * 当处理出错时（如 IMEI 无效、数据格式错误），告知设备出现了什么问题。
     * 格式：IW*{BP协议号}*{错误描述}#
     *
     * 这样设备可以知道服务端处理失败（有些设备会重新发送数据）。
     *
     * @param ctx 网络连接上下文
     * @param msg 原始消息（用于生成对应的响应协议号）
     * @param error 错误描述字符串（会传给设备）
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, WatchMessage msg, String error) {
        // 将 AP__ 替换为 BP__ 作为响应协议号
        String responseCode = msg.getProtocolCode().replace("AP", "BP");
        String response = "IW*" + responseCode + "*" + error + "#";
        ctx.writeAndFlush(response);
    }

    // ─── Netty Channel 生命周期方法 ──────────────────────────────────

    /**
     * 新设备连接成功时触发
     *
     * 【调用时机】
     * TCP 三次握手完成后，Netty 自动调用本方法（在发送任何业务数据之前）。
     *
     * 此时还不知道设备的 IMEI（IMEI 在第一个 AP00 登录包里才有），
     * 所以只记录日志，不做业务注册。
     *
     * 设备 IMEI 的注册发生在 channelRead0 中（收到有 IMEI 的消息时）。
     *
     * @param ctx 新建连接的上下文
     * @throws Exception Netty 框架要求声明
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // remoteAddress() = 设备的 IP 地址 + 端口，如 "/192.168.1.100:45678"
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.info("设备连接: {}", remoteAddress);
        // 注意：此时不知道 IMEI，无法注册到 DeviceManagerService
        // IMEI 注册在收到第一条消息（AP00 登录包）时完成
    }

    /**
     * 设备连接断开时触发
     *
     * 【调用时机】
     * TCP 四次挥手（正常断开）或连接超时（被 HeartbeatHandler 关闭）后，Netty 调用本方法。
     *
     * 主要任务：从 DeviceManagerService 中移除该设备的映射关系。
     * 这样其他代码就知道这个设备已经离线，发命令时不会尝试用已断开的 Channel。
     *
     * 【为什么这里能拿到 Channel？】
     * 虽然连接已断开，但 Channel 对象还在内存中（Netty 还没有 GC 它）。
     * 我们可以用这个 Channel 对象在 DeviceManagerService 里找到对应的 IMEI，
     * 然后清除双向映射。这就是为什么要维护 Channel→IMEI 的反向映射。
     *
     * @param ctx 断开连接的上下文
     * @throws Exception Netty 框架要求声明
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.info("设备断开: {}", remoteAddress);

        // 从设备管理器中注销（清除 IMEI↔Channel 双向映射）
        // DeviceManagerService.unregisterDevice() 会：
        //   1. 从 channelToImei 中找 IMEI
        //   2. 从 imeiToChannel 中删除该 IMEI
        //   3. 从 channelToImei 中删除该 Channel
        deviceManager.unregisterDevice(ctx.channel());
    }

    /**
     * Channel 出现异常时触发
     *
     * 【触发场景】
     * - 设备突然断电，TCP 连接中断（Read Timeout）
     * - 设备发来无法解析的数据（Decoder 异常向下传播）
     * - 网络不稳定导致的 IO 异常
     *
     * 【处理策略】
     * 记录异常日志，然后关闭连接（ctx.close()）。
     * 关闭连接会触发 channelInactive()，channelInactive 会清理 DeviceManagerService 中的映射。
     *
     * 注意：不要忘记 ctx.close()！如果异常后不关闭连接，这个"僵尸"连接会一直占用资源。
     *
     * @param ctx 发生异常的连接上下文
     * @param cause 异常原因
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("连接异常: {}", ctx.channel().remoteAddress(), cause);
        // 关闭连接，释放资源（会触发 channelInactive）
        ctx.close();
    }
}