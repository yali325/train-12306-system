-- KEYS[1]: rate:{date}:{train}; ARGV[1]: rate/s, ARGV[2]: burst capacity
local rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2]) * 1000
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens')) or capacity
local last = tonumber(redis.call('HGET', KEYS[1], 'last')) or now
tokens = math.min(capacity, tokens + math.max(0, now - last) * rate)
local allowed = 0
if tokens >= 1000 then
  tokens = tokens - 1000
  allowed = 1
end
redis.call('HSET', KEYS[1], 'tokens', tokens, 'last', now)
redis.call('EXPIRE', KEYS[1], 3600)
return allowed
