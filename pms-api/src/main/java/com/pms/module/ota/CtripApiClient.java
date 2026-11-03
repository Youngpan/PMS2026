package com.pms.module.ota;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONTokener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 携程直连 API 客户端。
 * 封装对携程 EBooking 接口的 HTTP 调用。
 */
@Slf4j
@Component
public class CtripApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 携程 EBK 接口域名 */
    private static final String CTRIP_BASE = "https://ebooking.ctrip.com";

    /** 订单查询接口路径 */
    private static final String QUERY_ORDER_LIST_URL = "/restapi/soa2/27204/queryOrderList";

    /** 默认风控用户代理 */
    private static final String DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * 模拟登录携程 EBK 接口（占位实现）
     * 真实场景需要 POST 到 login_url 获取 Cookie
     */
    public String doLogin(String accountName, String password) {
        // TODO: 发送 POST 到携程 EBK 登录接口，获取 Cookie
        // 返回值是 mock session token，真实场景应返回 Cookie 字符串
        return "mock_ctrip_session_" + UUID.randomUUID().toString().substring(0, 16);
    }

    /**
     * 调用携程 queryOrderList 接口拉取订单
     *
     * @param sessionToken  解密后的 session/cookie
     * @param pageIndex     页码(0-based)
     * @param pageSize      每页条数(默认20)
     * @return 订单列表JSONArray
     */
    public JSONArray queryOrderList(String sessionToken, int pageIndex, int pageSize) {
        HttpRequest req = HttpUtil.createPost(CTRIP_BASE + QUERY_ORDER_LIST_URL)
                .header("Cookie", sessionToken)
                .header("User-Agent", getUA())
                .header("Content-Type", "application/json;charset=UTF-8")
                .timeout(15000);

        // 构建请求体（参考文档8.1 queryOrderList 接口结构）
        JSONObject body = new JSONObject();
        body.put("orderQueryCondition", new JSONObject()
                .put("pageInfo", new JSONObject()
                        .put("pageIndex", pageIndex)
                        .put("pageSize", pageSize)));
        body.put("pageIndexReq", pageIndex);
        body.put("pageSizeReq", pageSize);
        body.put("orderBy", "FormDate");
        body.put("sort", "Desc");

        try {
            req.body(body.toString());
            HttpResponse resp = req.execute();
            log.debug("[CtripApi] queryOrderList status={}, body={}", resp.getStatus(), resp.body());

            if (resp.getStatus() == 200) {
                JSONObject json = new JSONObject(resp.body());
                int resultCode = json.getInt("resultCode");
                if (resultCode != 0) {
                    // 需要检测是否是登录态过期
                    String msg = json.getStr("message");
                    if (resp.getStatus() == 401 || resp.getStatus() == 403 || msg.contains("not login") || msg.contains("expire") || msg.contains("auth")) {
                        throw new BusinessException("登录凭证已过期，需要重新登录");
                    }
                    throw new BusinessException("携程接口返回错误: " + msg);
                }
                JSONObject data = json.getJSONObject("data");
                if (data == null) return new JSONArray();
                return data.getJSONArray("orderList");
            } else {
                throw new BusinessException("HTTP请求失败: status=" + resp.getStatus());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("queryOrderList 调用异常: " + e.getMessage());
        }
    }

    /**
     * 保活接口（模拟调用 queryOrderList 取第一页1条，用于维持 session）
     */
    public boolean keepaliveRequest(String sessionToken) {
        try {
            JSONArray orders = queryOrderList(sessionToken, 0, 1);
            log.debug("[CtripApi] keepalive success, got {} orders", orders.size());
            return true;
        } catch (Exception e) {
            log.warn("[CtripApi] keepalive failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取随机抖动的 User-Agent（±5s抖动实现参考风控策略）
     */
    private String getUA() {
        return DEFAULT_UA;
    }

    /**
     * 获取随机抖动后的 User-Agent（带抖动）
     */
    public String getUAWithJitter() {
        // 风控对抗策略：固定UA基础上随机抖动±5s（这里只是占位）
        return DEFAULT_UA;
    }
}
