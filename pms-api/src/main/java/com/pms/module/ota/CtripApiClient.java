package com.pms.module.ota;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 携程 Ebooking API 客户端封装。
 *
 * 接口参考：https://ebooking.ctrip.com/restapi/soa2/27204/queryOrderList
 *
 * 职责：
 * - 构造携程 queryOrderList 请求
 * - 处理 Cookie / token 认证
 * - 解析响应，转换为 ChannelOrder 列表
 * - 风控策略：固定 UA、请求抖动、429 降级
 */
@Slf4j
public class CtripApiClient {

    private static final String QUERY_ORDER_URL = "https://ebooking.ctrip.com/restapi/soa2/27204/queryOrderList";
    private static final String DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final Random RANDOM = new Random();

    /** 请求间隔抖动 ±5 秒 */
    private static final int JITTER_MS = 5000;

    /** 风控降级倍数 */
    private static final int RATE_LIMIT_BACKOFF = 2;

    /** 页面大小 */
    private static final int PAGE_SIZE = 20;

    private final OtaAccount account;

    /** 当前降级因子（指数退避，遇到 429 时加倍） */
    private int backoffFactor = 1;

    public CtripApiClient(OtaAccount account) {
        this.account = account;
    }

    public OtaAccount getAccount() {
        return account;
    }

    /**
     * 解析账户中存储的风控配置
     */
    public String resolveUserAgent() {
        try {
            String config = account.getServerConfig();
            if (config != null && !config.isEmpty()) {
                JSONObject jo = JSONUtil.parseObj(config);
                String ua = jo.getStr("userAgent");
                if (ua != null && !ua.isEmpty()) return ua;
            }
        } catch (Exception ignored) {}
        return DEFAULT_UA;
    }

    /**
     * 执行请求加入随机抖动休眠
     */
    private void applyJitter() {
        int jitter = RANDOM.nextInt(JITTER_MS * 2) - JITTER_MS; // -5s ~ +5s
        if (jitter > 0) {
            try {
                Thread.sleep(jitter);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 单次保活请求：拉取最新 1 条订单，验证 session 是否存活
     * @return true 表示 session 正常
     */
    public boolean keepalive() {
        try {
            String token = account.getSessionToken();
            if (token == null || token.isEmpty()) return false;

            // 构造最小量请求：pageSize=1, pageIndex=0
            JSONObject body = buildQueryBody(0, 1, null);
            applyJitter();
            HttpResponse resp = HttpRequest.post(QUERY_ORDER_URL)
                    .header("User-Agent", resolveUserAgent())
                    .header("Cookie", decryptToken(token))
                    .body(body.toString())
                    .timeout(10000)
                    .execute();

            int status = resp.getStatus();
            if (status == 200) {
                JSONObject jo = JSONUtil.parseObj(resp.body());
                if (jo.containsKey("orderList")) {
                    return true;
                }
            }
            log.warn("[CtripApi] keepalive failed status={}", status);
            return false;
        } catch (Exception e) {
            log.warn("[CtripApi] keepalive exception", e);
            return false;
        }
    }

    /**
     * 拉取订单列表（增量模式）
     *
     * @param pageIndex 从 0 开始的页码
     * @param watermark 水位线时间（ISO 格式），只返回该时间之后的订单；null 表示全量
     * @return 订单列表
     */
    public List<ChannelOrder> fetchOrderPage(int pageIndex, String watermark) {
        applyJitter();

        JSONObject body = buildQueryBody(pageIndex, PAGE_SIZE, watermark);
        try {
            HttpResponse resp = HttpRequest.post(QUERY_ORDER_URL)
                    .header("User-Agent", resolveUserAgent())
                    .header("Cookie", decryptToken(account.getSessionToken()))
                    .body(body.toString())
                    .timeout(15000)
                    .execute();

            int status = resp.getStatus();

            // 风控降级：429 Too Many Requests
            if (status == 429 || status == 503) {
                backoffFactor = Math.min(backoffFactor * RATE_LIMIT_BACKOFF, 16);
                log.warn("[CtripApi] rate limited status={}, backoff={}", status, backoffFactor);
                return List.of(); // 返回空列表，调用方应增加间隔
            }

            // 认证失败
            if (status == 401 || status == 403) {
                throw new SecurityException("携程登录态失效 status=" + status);
            }

            if (status != 200) {
                log.warn("[CtripApi] queryOrderList status={}", status);
                return List.of();
            }

            // 重置降级因子
            backoffFactor = 1;

            return parseOrderList(resp.body(), pageIndex);
        } catch (Exception e) {
            log.error("[CtripApi] fetchOrderPage error page={}", pageIndex, e);
            throw new RuntimeException("携程接口调用失败", e);
        }
    }

    /**
     * 获取当前降级因子（调用方应乘以基础间隔）
     */
    public int getBackoffFactor() {
        return backoffFactor;
    }

    /**
     * 构建 queryOrderList 请求体
     */
    private JSONObject buildQueryBody(int pageIndex, int pageSize, String watermark) {
        JSONObject body = JSONUtil.createObj();

        // 订单查询条件
        JSONObject condition = JSONUtil.createObj();
        condition.set("orderBy", "FormDate");
        condition.set("sort", "Desc");

        // 分页
        JSONObject pageInfo = JSONUtil.createObj()
                .set("pageIndex", pageIndex)
                .set("pageSize", pageSize);
        condition.set("pageInfo", pageInfo);

        // 增量过滤：只查水位线之后的订单
        if (watermark != null && !watermark.isEmpty()) {
            condition.set("formDate", watermark);
        }

        // 订单查询条件
        JSONObject queryCondition = JSONUtil.createObj();
        queryCondition.set("orderQueryCondition", condition);

        return queryCondition;
    }

    /**
     * 解析订单列表响应
     */
    private List<ChannelOrder> parseOrderList(String json, int pageIndex) {
        List<ChannelOrder> list = new ArrayList<>();

        try {
            JSONObject root = JSONUtil.parseObj(json);
            JSONArray orderList = root.getJSONArray("orderList");
            if (orderList == null || orderList.isEmpty()) return list;

            // 提取时间作为新的水位线
            for (Object obj : orderList) {
                JSONObject item = (JSONObject) obj;
                ChannelOrder co = new ChannelOrder();
                co.setHotelId(account.getHotelId());
                co.setOtaAccountId(account.getId());
                co.setPlatform(account.getPlatform());
                co.setSourceType("Ebooking");

                // 核心字段
                co.setChannelOrderId(item.getStr("orderId"));
                co.setGuestName(item.getStr("clientName"));
                co.setChannelStatus(mapChannelStatus(item.getStr("orderStatusType")));

                // 日期
                String arrival = item.getStr("arrival");
                String departure = item.getStr("departure");
                if (arrival != null) {
                    co.setCheckInDate(LocalDate.parse(arrival.substring(0, 10)));
                }
                if (departure != null) {
                    co.setCheckOutDate(LocalDate.parse(departure.substring(0, 10)));
                }
                // 晚数
                if (co.getCheckInDate() != null && co.getCheckOutDate() != null) {
                    co.setNights((int) java.time.temporal.ChronoUnit.DAYS.between(co.getCheckInDate(), co.getCheckOutDate()));
                }

                // 人数
                JSONObject personInfo = item.getJSONObject("personCountInfo");
                if (personInfo != null) {
                    Integer adult = personInfo.getInt("adult");
                    Integer child = personInfo.getInt("child");
                    co.setPersons((adult != null ? adult : 0) + (child != null ? child : 0));
                }

                // 房型
                co.setRoomTypeName(item.getStr("roomTypeName"));

                // 金额
                String total = item.getStr("totalPrice");
                if (total != null && !total.isEmpty()) {
                    co.setTotalAmount(new BigDecimal(total));
                }

                // 原始 JSON 快照
                co.setRawJson(item.toString());
                co.setProcessStatus("RAW");
                co.setCreatedAt(LocalDateTime.now());

                list.add(co);
            }
        } catch (Exception e) {
            log.error("[CtripApi] 解析订单列表失败 page={}, json={}", pageIndex, json.length() > 200 ? json.substring(0,200) : json, e);
        }

        return list;
    }

    /**
     * 映射携程订单状态 → 百居易状态
     * Receipt → 已确认(待入住), CheckIn → 已入住, Canceled → 已取消
     */
    private String mapChannelStatus(String ctripStatus) {
        if (ctripStatus == null) return null;
        switch (ctripStatus) {
            case "Receipt": return "CONFIRMED";
            case "CheckIn": return "CHECK_IN";
            case "Canceled": return "CANCELED";
            default: return ctripStatus;
        }
    }

    /**
     * 解密 token（占位，将来对接 hutool AES 解密）
     * 实际调用 OtaAccountService 的同名方法
     */
    private static String decryptToken(String encrypted) {
        // 委托给 OtaAccountService 的相同方法
        // 由于该方法是 private 且不在本类中，此处用工具类代替
        // 真实场景应传入 token 直接作为 Cookie 使用
        // 如果 sessionToken 是加密的，需要先解密
        // 当前简单实现：直接作为 Cookie 返回
        return encrypted;
    }
}
