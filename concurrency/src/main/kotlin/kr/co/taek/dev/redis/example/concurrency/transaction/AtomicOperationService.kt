package kr.co.taek.dev.redis.example.concurrency.transaction

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SessionCallback
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service
import kotlin.math.pow
import kotlin.random.Random

private val log = KotlinLogging.logger {}

@Service
class AtomicOperationService(
    private val redisTemplate: RedisTemplate<String, Int>,
) {
    fun increment(
        key: String,
        retryable: Boolean = false,
        retryCnt: Int = 3,
        backOff: Long = 1000L,
    ) {
        if (retryable) {
            var retry = 0
            while (retry < retryCnt) {
                try {
                    incrementWithRedisCallback(key)
                    break
                } catch (e: Exception) {
                    log.error { "Error: $e" }
                    retry++

                    var backoffTime = backOff * 2.00.pow(retry.toDouble()).toLong()
                    backoffTime += Random.nextInt(100) // 무작위성 추가, thundering herd problem (즉, 여러 클라이언트가 동시에 재시도를 하는 문제) 방지

                    try {
                        Thread.sleep(backoffTime) // 계산된 시간만큼 대기
                    } catch (ie: InterruptedException) {
                        log.warn { "Thread interrupted during backoff sleep" }
                        Thread.currentThread().interrupt() // 현재 스레드의 인터럽트 상태를 설정
                        break
                    }
                }
            }
        } else {
            incrementWithRedisCallback(key)
        }
    }

    private fun incrementWithRedisCallback(key: String) {
        val result =
            redisTemplate.execute(
                object : SessionCallback<List<Any>> {
                    override fun <K : Any, V : Any> execute(operations: RedisOperations<K, V>): List<Any> {
                        redisTemplate.watch(key)
                        val currentValue = redisTemplate.opsForValue().get(key) ?: 0
                        redisTemplate.multi()
                        return try {
                            redisTemplate.opsForValue().set(key, currentValue + 1)
                            redisTemplate.exec()
                        } catch (e: Exception) {
                            log.error { "Error: $e" }
                            redisTemplate.discard()
                            throw e
                        } finally {
                            redisTemplate.unwatch()
                        }
                    }
                },
            )

        if (result.isEmpty()) {
            log.error { "트랜잭션 실패: 다른 클라이언트에 의해 키가 변경되었습니다." }
            throw RuntimeException("트랜잭션 실패: 다른 클라이언트에 의해 키가 변경되었습니다.")
        }
    }

    fun incrementWithLuaScript(key: String) {
        val script = "local currentValue = redis.call('GET', KEYS[1]) " +
            "if currentValue == false then " +
            "currentValue = 0 " +
            "else " +
            "currentValue = tonumber(currentValue) " +
            "end " +
            "redis.call('SET', KEYS[1], currentValue + 1) " +
            "return currentValue + 1"

        val result = redisTemplate.execute(
            RedisScript.of(script, Long::class.java), // Lua script to execute
            listOf(key) // KEYS[1] is the key in Lua script
        )

        log.info { "Increment result: $result" }
    }
}
