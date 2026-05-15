local key = KEYS[1]
local amount = tonumber(ARGV[1])

redis.call('INCRBY', key, amount)
return redis.call('GET', key)