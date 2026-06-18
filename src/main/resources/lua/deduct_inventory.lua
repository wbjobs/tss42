-- 扣减库存Lua脚本
-- KEYS[1]: 库存key (inventory:stock:{productId})
-- KEYS[2]: 预占库存明细key (inventory:reserve:{orderNo})
-- ARGV[1]: 扣减数量
-- ARGV[2]: 订单号
-- ARGV[3]: 过期时间(秒)
-- 返回: 1-成功 0-库存不足 -1-重复扣减

local stockKey = KEYS[1]
local reserveKey = KEYS[2]
local quantity = tonumber(ARGV[1])
local orderNo = ARGV[2]
local expireTime = tonumber(ARGV[3])

local exists = redis.call('EXISTS', reserveKey)
if exists == 1 then
    return -1
end

local availableStock = tonumber(redis.call('HGET', stockKey, 'availableStock') or '0')
if availableStock < quantity then
    return 0
end

redis.call('HINCRBY', stockKey, 'availableStock', -quantity)
redis.call('HINCRBY', stockKey, 'reservedStock', quantity)

redis.call('HSET', reserveKey, 'productId', string.sub(stockKey, string.len('inventory:stock:') + 1))
redis.call('HSET', reserveKey, 'quantity', quantity)
redis.call('HSET', reserveKey, 'orderNo', orderNo)
redis.call('HSET', reserveKey, 'status', 'RESERVED')
redis.call('EXPIRE', reserveKey, expireTime)

return 1
