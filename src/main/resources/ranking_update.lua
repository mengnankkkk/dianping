-- 参数说明：
-- ARGV[1] = member
-- ARGV[2] = base_score
-- ARGV[3] = key1
-- ARGV[4] = weight1
-- ARGV[5] = key2
-- ARGV[6] = weight2
-- 以此类推...

local member = ARGV[1]
local base_score = tonumber(ARGV[2])

for i = 3, #ARGV, 2 do
    local key = ARGV[i]
    local weight = tonumber(ARGV[i + 1])
    local weighted_score = base_score * weight
    redis.call('ZADD', key, weighted_score, member)
end

return 'OK'
