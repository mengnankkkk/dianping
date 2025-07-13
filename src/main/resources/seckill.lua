-- stream-seckill.lua
-- KEYS[1]: voucherId
-- ARGV[1]: userId
local voucherId = KEYS[1]  -- 优惠券ID
local userId = ARGV[1]    -- 用户ID
-- local orderId = ARGV[2]; -- 订单Id
-- 库存的key
local stockKey = 'seckill:stock:' .. voucherId
-- 订单key
local orderKey = 'seckill:order:' .. voucherId
-- 1. 判断库存是否充足
-- get stockKey
-- stock > 0
local stock = redis.call('get', stockKey)
if (tonumber(stock) <= 0) then
  -- 库存不足，返回1
  return 1
end
-- 2. 判断用户是否已经下单
-- SISMEMBER orderKey userId
if (redis.call('sismember', orderKey, userId) == 1) then
  -- 用户已下单，返回2
  return 2
end
-- 3. 扣库存、下单
-- INCRBY stockKey -1
redis.call('INCRBY', stockKey, -1)
-- SADD orderKey userId
redis.call('sadd', orderKey, userId)
return 0;