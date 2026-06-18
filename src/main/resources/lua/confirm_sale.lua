-- 确认销售Lua脚本
-- KEYS[1]: 库存key (inventory:stock:{productId})
-- KEYS[2]: 预占库存明细key (inventory:reserve:{orderNo})
-- ARGV[1]: 确认数量
-- 返回: 1-成功 0-预占记录不存在 -1-重复确认

local stockKey = KEYS[1]
local reserveKey = KEYS[2]
local quantity = tonumber(ARGV[1])

local exists = redis.call('EXISTS', reserveKey)
if exists == 0 then
    return 0
end

local status = redis.call('HGET', reserveKey, 'status')
if status == 'CONFIRMED' then
    return -1
end

redis.call('HINCRBY', stockKey, 'reservedStock', -quantity)
redis.call('HINCRBY', stockKey, 'soldStock', quantity)

redis.call('HSET', reserveKey, 'status', 'CONFIRMED')
redis.call('DEL', reserveKey)

return 1
