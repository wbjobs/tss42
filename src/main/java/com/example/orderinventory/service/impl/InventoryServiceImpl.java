package com.example.orderinventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.orderinventory.entity.Inventory;
import com.example.orderinventory.entity.InventoryLog;
import com.example.orderinventory.mapper.InventoryLogMapper;
import com.example.orderinventory.mapper.InventoryMapper;
import com.example.orderinventory.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    private static final String STOCK_KEY_PREFIX = "inventory:stock:";
    private static final String RESERVE_KEY_PREFIX = "inventory:reserve:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private InventoryMapper inventoryMapper;

    @Resource
    private InventoryLogMapper inventoryLogMapper;

    private DefaultRedisScript<Long> deductScript;
    private DefaultRedisScript<Long> rollbackScript;
    private DefaultRedisScript<Long> confirmScript;

    @PostConstruct
    public void init() {
        deductScript = new DefaultRedisScript<>();
        deductScript.setResultType(Long.class);
        deductScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/deduct_inventory.lua")));

        rollbackScript = new DefaultRedisScript<>();
        rollbackScript.setResultType(Long.class);
        rollbackScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rollback_inventory.lua")));

        confirmScript = new DefaultRedisScript<>();
        confirmScript.setResultType(Long.class);
        confirmScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/confirm_sale.lua")));
    }

    @Override
    public boolean deductInventory(Long productId, Integer quantity, String orderNo) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        String reserveKey = RESERVE_KEY_PREFIX + orderNo;

        ensureStockInRedis(productId, stockKey);

        List<String> keys = Arrays.asList(stockKey, reserveKey);
        Long result = stringRedisTemplate.execute(deductScript, keys,
                String.valueOf(quantity),
                orderNo,
                String.valueOf(900));

        if (result == null) {
            log.error("扣减库存Lua脚本执行失败, productId: {}, orderNo: {}", productId, orderNo);
            return false;
        }

        if (result == 1) {
            log.info("扣减库存成功, productId: {}, quantity: {}, orderNo: {}", productId, quantity, orderNo);
            return true;
        } else if (result == 0) {
            log.warn("库存不足, productId: {}, quantity: {}, orderNo: {}", productId, quantity, orderNo);
            return false;
        } else if (result == -1) {
            log.warn("重复扣减, orderNo: {}", orderNo);
            return true;
        }
        return false;
    }

    @Override
    public boolean rollbackInventory(Long productId, Integer quantity, String orderNo) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        String reserveKey = RESERVE_KEY_PREFIX + orderNo;

        List<String> keys = Arrays.asList(stockKey, reserveKey);
        Long result = stringRedisTemplate.execute(rollbackScript, keys, String.valueOf(quantity));

        if (result == null) {
            log.error("归还库存Lua脚本执行失败, productId: {}, orderNo: {}", productId, orderNo);
            return false;
        }

        if (result == 1) {
            log.info("归还库存成功, productId: {}, quantity: {}, orderNo: {}", productId, quantity, orderNo);
            return true;
        } else if (result == 0) {
            log.warn("预占记录不存在, orderNo: {}", orderNo);
            return true;
        } else if (result == -1) {
            log.warn("重复归还, orderNo: {}", orderNo);
            return true;
        }
        return false;
    }

    @Override
    public boolean confirmSale(Long productId, Integer quantity, String orderNo) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        String reserveKey = RESERVE_KEY_PREFIX + orderNo;

        List<String> keys = Arrays.asList(stockKey, reserveKey);
        Long result = stringRedisTemplate.execute(confirmScript, keys, String.valueOf(quantity));

        if (result == null) {
            log.error("确认销售Lua脚本执行失败, productId: {}, orderNo: {}", productId, orderNo);
            return false;
        }

        if (result == 1) {
            log.info("确认销售成功, productId: {}, quantity: {}, orderNo: {}", productId, quantity, orderNo);
            return true;
        } else if (result == 0) {
            log.warn("预占记录不存在, orderNo: {}", orderNo);
            return true;
        } else if (result == -1) {
            log.warn("重复确认, orderNo: {}", orderNo);
            return true;
        }
        return false;
    }

    @Override
    public Inventory getInventoryByProductId(Long productId) {
        QueryWrapper<Inventory> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId);
        return inventoryMapper.selectOne(wrapper);
    }

    @Override
    public void addInventoryLog(InventoryLog log) {
        inventoryLogMapper.insert(log);
    }

    private void ensureStockInRedis(Long productId, String stockKey) {
        Boolean hasKey = stringRedisTemplate.hasKey(stockKey);
        if (Boolean.FALSE.equals(hasKey)) {
            Inventory inventory = getInventoryByProductId(productId);
            if (inventory != null) {
                stringRedisTemplate.opsForHash().put(stockKey, "availableStock", String.valueOf(inventory.getAvailableStock()));
                stringRedisTemplate.opsForHash().put(stockKey, "reservedStock", String.valueOf(inventory.getReservedStock()));
                stringRedisTemplate.opsForHash().put(stockKey, "soldStock", String.valueOf(inventory.getSoldStock()));
            }
        }
    }
}
