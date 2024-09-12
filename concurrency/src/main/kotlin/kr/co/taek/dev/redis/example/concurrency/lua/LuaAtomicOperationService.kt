package kr.co.taek.dev.redis.example.concurrency.lua

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class LuaAtomicOperationService(
    private val redisTemplate: RedisTemplate<String, Int>,
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    fun incrementWithLuaScript(key: String) {
        val script =
            "local currentValue = redis.call('GET', KEYS[1]) " +
                "if currentValue == false then " +
                "currentValue = 0 " +
                "else " +
                "currentValue = tonumber(currentValue) " +
                "end " +
                "redis.call('SET', KEYS[1], currentValue + 1) " +
                "return currentValue + 1"

        val result =
            redisTemplate.execute(
                RedisScript.of(script, Long::class.java), // Lua script to execute
                listOf(key), // KEYS[1] is the key in Lua script
            )

        log.info { "Increment result: $result" }
    }

    fun <T> updateJsonFieldWithLuaScript(
        redisKey: String,
        jsonField: String,
        newValue: T,
    ): T {
        val input = objectMapper.writeValueAsString(newValue)

        // Lua script to update JSON field
        val script =
            """
            local currentValue = redis.call('GET', KEYS[1])
            local inputValue = cjson.decode(ARGV[2])

            if not currentValue then
                -- If key does not exist, set it with the provided input
                redis.call('SET', KEYS[1], cjson.encode(inputValue))
                return cjson.encode(inputValue)
            end

            -- Decode existing value as JSON
            local success, json = pcall(cjson.decode, currentValue)
            if not success then
                error('Error decoding JSON')
            end

            -- Check if the field is empty and update
            if json[ARGV[1]] == nil or json[ARGV[1]] == cjson.null then
                json[ARGV[1]] = inputValue[ARGV[1]]
                redis.call('SET', KEYS[1], cjson.encode(json))
                return cjson.encode(json)
            else
                error('Field already has a value')
            end
            """.trimIndent()

        val result =
            stringRedisTemplate.execute(
                RedisScript.of(script, String::class.java),
                RedisSerializer.string(),
                RedisSerializer.string(),
                listOf(redisKey),
                jsonField,
                input,
            )
        log.info { "Result: $result" }

        return objectMapper.readValue(result, newValue!!::class.java)
    }
}
