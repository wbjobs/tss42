package com.example.orderinventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.orderinventory.dto.RiskScoreRequest;
import com.example.orderinventory.dto.RiskScoreResult;
import com.example.orderinventory.entity.Order;
import com.example.orderinventory.enums.OrderStatusEnum;
import com.example.orderinventory.mapper.OrderMapper;
import com.example.orderinventory.service.RiskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RiskServiceImpl implements RiskService {

    private static final String IP_RISK_KEY = "risk:ip:";
    private static final String DEVICE_RISK_KEY = "risk:device:";
    private static final String IP_ORDER_COUNT_KEY = "risk:ip:order:count:";
    private static final String DEVICE_ORDER_COUNT_KEY = "risk:device:order:count:";

    @Value("${order.risk-threshold:60}")
    private int riskThreshold;

    @Value("${order.timeout:900}")
    private int normalTimeoutSeconds;

    @Value("${order.high-risk-timeout:300}")
    private int highRiskTimeoutSeconds;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public RiskScoreResult calculateRiskScore(RiskScoreRequest request) {
        int totalScore = 0;
        StringBuilder riskReason = new StringBuilder();

        int ipScore = calculateIpRisk(request.getIp());
        if (ipScore > 0) {
            riskReason.append("IP风险(").append(ipScore).append("分); ");
        }
        totalScore += ipScore;

        int deviceScore = calculateDeviceRisk(request.getDeviceFingerprint());
        if (deviceScore > 0) {
            riskReason.append("设备风险(").append(deviceScore).append("分); ");
        }
        totalScore += deviceScore;

        int behaviorScore = calculateUserBehaviorRisk(request.getUserId());
        if (behaviorScore > 0) {
            riskReason.append("用户行为风险(").append(behaviorScore).append("分); ");
        }
        totalScore += behaviorScore;

        int frequencyScore = calculateFrequencyRisk(request.getIp(), request.getDeviceFingerprint());
        if (frequencyScore > 0) {
            riskReason.append("操作频率风险(").append(frequencyScore).append("分); ");
        }
        totalScore += frequencyScore;

        boolean highRisk = totalScore >= riskThreshold;
        String riskLevel = getRiskLevel(totalScore);
        int reserveSeconds = highRisk ? highRiskTimeoutSeconds : normalTimeoutSeconds;

        RiskScoreResult result = RiskScoreResult.builder()
                .score(totalScore)
                .highRisk(highRisk)
                .riskLevel(riskLevel)
                .riskReason(riskReason.length() > 0 ? riskReason.toString() : "正常")
                .reserveSeconds(reserveSeconds)
                .build();

        log.info("[风控评分] userId: {}, ip: {}, device: {}, score: {}, highRisk: {}, reason: {}",
                request.getUserId(), request.getIp(), request.getDeviceFingerprint(),
                totalScore, highRisk, result.getRiskReason());

        return result;
    }

    private int calculateIpRisk(String ip) {
        if (ip == null || ip.isEmpty()) {
            return 20;
        }

        String riskKey = IP_RISK_KEY + ip;
        String riskLevel = stringRedisTemplate.opsForValue().get(riskKey);
        if ("BLACK".equals(riskLevel)) {
            return 100;
        } else if ("WARN".equals(riskLevel)) {
            return 30;
        }

        if (isProxyIp(ip)) {
            return 25;
        }

        return 0;
    }

    private int calculateDeviceRisk(String deviceFingerprint) {
        if (deviceFingerprint == null || deviceFingerprint.isEmpty()) {
            return 15;
        }

        String riskKey = DEVICE_RISK_KEY + deviceFingerprint;
        String riskLevel = stringRedisTemplate.opsForValue().get(riskKey);
        if ("BLACK".equals(riskLevel)) {
            return 90;
        } else if ("WARN".equals(riskLevel)) {
            return 25;
        }

        long deviceUserCount = countUsersByDevice(deviceFingerprint);
        if (deviceUserCount > 5) {
            return 30;
        } else if (deviceUserCount > 2) {
            return 15;
        }

        return 0;
    }

    private int calculateUserBehaviorRisk(Long userId) {
        if (userId == null) {
            return 10;
        }

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .ge("create_time", sevenDaysAgo);
        List<Order> last7DaysOrders = orderMapper.selectList(queryWrapper);

        if (last7DaysOrders == null || last7DaysOrders.isEmpty()) {
            return 0;
        }

        int totalOrders = last7DaysOrders.size();
        int cancelledOrders = 0;
        for (Order order : last7DaysOrders) {
            if (OrderStatusEnum.CANCELLED.getCode().equals(order.getStatus())
                    || OrderStatusEnum.TIMEOUT_CANCELLED.getCode().equals(order.getStatus())) {
                cancelledOrders++;
            }
        }

        double cancelRate = (double) cancelledOrders / totalOrders;
        if (cancelRate >= 0.8) {
            return 50;
        } else if (cancelRate >= 0.5) {
            return 30;
        } else if (cancelRate >= 0.3) {
            return 15;
        }

        if (cancelledOrders >= 5) {
            return 20;
        }

        return 0;
    }

    private int calculateFrequencyRisk(String ip, String deviceFingerprint) {
        int score = 0;

        if (ip != null && !ip.isEmpty()) {
            String ipCountKey = IP_ORDER_COUNT_KEY + ip;
            Long ipCount = stringRedisTemplate.opsForValue().increment(ipCountKey);
            if (ipCount != null && ipCount == 1) {
                stringRedisTemplate.expire(ipCountKey, 5, TimeUnit.MINUTES);
            }
            if (ipCount != null && ipCount > 20) {
                score += 40;
            } else if (ipCount != null && ipCount > 10) {
                score += 20;
            }
        }

        if (deviceFingerprint != null && !deviceFingerprint.isEmpty()) {
            String deviceCountKey = DEVICE_ORDER_COUNT_KEY + deviceFingerprint;
            Long deviceCount = stringRedisTemplate.opsForValue().increment(deviceCountKey);
            if (deviceCount != null && deviceCount == 1) {
                stringRedisTemplate.expire(deviceCountKey, 5, TimeUnit.MINUTES);
            }
            if (deviceCount != null && deviceCount > 15) {
                score += 30;
            } else if (deviceCount != null && deviceCount > 8) {
                score += 15;
            }
        }

        return score;
    }

    private boolean isProxyIp(String ip) {
        return false;
    }

    private long countUsersByDevice(String deviceFingerprint) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT user_id")
                .eq("device_fingerprint", deviceFingerprint)
                .ge("create_time", LocalDateTime.now().minusDays(30));
        return orderMapper.selectCount(queryWrapper).longValue();
    }

    private String getRiskLevel(int score) {
        if (score >= 80) {
            return "HIGH";
        } else if (score >= 60) {
            return "MEDIUM_HIGH";
        } else if (score >= 40) {
            return "MEDIUM";
        } else if (score >= 20) {
            return "LOW_MEDIUM";
        } else {
            return "LOW";
        }
    }
}
