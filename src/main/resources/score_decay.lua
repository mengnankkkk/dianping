-- KEYS[1] = 有序集合 key
-- ARGV[1] = decay_factor (衰减因子，如 0.9)
-- ARGV[2] = min_score (最小保留分数，低于此值将被移除)

local key = KEYS[1]
local decay_factor = tonumber(ARGV[1])
local min_score = tonumber(ARGV[2])

local members = redis.call('ZRANGE', key, 0, -1, 'WITHSCORES')
local updated = 0

for i = 1, #members, 2 do
    local member = members[i]
    local score = tonumber(members[i + 1])
    local new_score = score * decay_factor
    if new_score >= min_score then
        redis.call('ZADD', key, new_score, member)
        updated = updated + 1
    else
        redis.call('ZREM', key, member)
    end
end

return updated
