local key = KEYS[1]
local amount = tonumber(ARGV[1])

local balance = tonumber(redis.call('GET', key))

if balance == nil then
    return -1
end

if balance < amount then
    return -2
end

redis.call('DECRBY', key, amount)
return redis.call('GET', key)