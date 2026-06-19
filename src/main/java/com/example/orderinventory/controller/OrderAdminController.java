package com.example.orderinventory.controller;

import com.example.orderinventory.common.Result;
import com.example.orderinventory.dto.ManualReleaseRequest;
import com.example.orderinventory.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/admin/order")
public class OrderAdminController {

    @Resource
    private OrderService orderService;

    @PostMapping("/release")
    public Result<Void> manualReleaseInventory(
            @RequestBody @Validated ManualReleaseRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ip = getClientIp(httpRequest);
            log.info("[管理后台-手动释放库存] operator: {}({}), orderNo: {}, ip: {}",
                    request.getOperatorName(), request.getOperatorNo(),
                    request.getOrderNo(), ip);

            boolean result = orderService.manualReleaseOrder(
                    request.getOrderNo(),
                    request.getOperatorNo(),
                    request.getOperatorName(),
                    ip
            );

            if (result) {
                return Result.success();
            } else {
                return Result.fail("释放库存失败，订单状态可能已变更");
            }
        } catch (Exception e) {
            log.error("[管理后台-手动释放库存] 失败, orderNo: {}", request.getOrderNo(), e);
            return Result.fail(e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
