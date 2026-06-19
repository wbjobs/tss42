package com.example.orderinventory.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.orderinventory.dto.InventoryChangeEvent;
import com.example.orderinventory.service.ElasticsearchService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Value("${elasticsearch.index.inventory-change:inventory_change_log}")
    private String inventoryChangeIndex;

    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Override
    @Async("taskExecutor")
    public void saveInventoryChangeAsync(InventoryChangeEvent event) {
        try {
            log.info("[ES-异步写入] 库存变动事件, eventId: {}, type: {}, orderNo: {}",
                    event.getEventId(), event.getOperationType(), event.getOrderNo());

            Map<String, Object> source = buildSource(event);
            String indexName = buildIndexName();

            IndexRequest request = new IndexRequest(indexName);
            request.id(event.getEventId());
            request.source(source, XContentType.JSON);

            IndexResponse response = restHighLevelClient.index(request, RequestOptions.DEFAULT);
            log.info("[ES-异步写入] 成功, eventId: {}, index: {}, status: {}",
                    event.getEventId(), indexName, response.status());
        } catch (Exception e) {
            log.error("[ES-异步写入] 失败, eventId: {}, type: {}, orderNo: {}",
                    event.getEventId(), event.getOperationType(), event.getOrderNo(), e);
        }
    }

    private Map<String, Object> buildSource(InventoryChangeEvent event) {
        Map<String, Object> source = new HashMap<>();
        source.put("eventId", event.getEventId());
        source.put("operationType", event.getOperationType());
        source.put("orderNo", event.getOrderNo());
        source.put("productId", event.getProductId());
        source.put("userId", event.getUserId());
        source.put("quantity", event.getQuantity());
        source.put("operatorNo", event.getOperatorNo());
        source.put("operatorName", event.getOperatorName());
        source.put("ip", event.getIp());
        source.put("deviceFingerprint", event.getDeviceFingerprint());
        source.put("riskScore", event.getRiskScore());
        source.put("riskFlag", event.getRiskFlag());
        source.put("beforeStatus", event.getBeforeStatus());
        source.put("afterStatus", event.getAfterStatus());
        source.put("remark", event.getRemark());
        source.put("source", event.getSource());
        if (event.getOccurTime() != null) {
            source.put("occurTime", event.getOccurTime().format(FORMATTER));
        } else {
            source.put("occurTime", LocalDateTime.now().format(FORMATTER));
        }
        source.put("@timestamp", LocalDateTime.now().format(FORMATTER));
        return source;
    }

    private String buildIndexName() {
        String dateSuffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        return inventoryChangeIndex + "-" + dateSuffix;
    }
}
