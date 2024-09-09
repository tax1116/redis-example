package kr.co.taek.dev.redis.example.concurrency.transaction

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CompletableFuture

@ActiveProfiles("test")
@SpringBootTest
class AtomicOperationServiceTests {
    @Autowired
    private lateinit var atomicOperationService: AtomicOperationService

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, Int>

    @DisplayName("2개의 스레드가 동시에 Redis에 접근하여 값을 증가시키더라도 레디스 트랜잭션에 의해 값이 각각 1씩 증가한다.")
    @Test
    fun increment_with_redis_transaction() {
        // given
        val key = "test"

        redisTemplate.opsForValue().get(key)?.let {
            redisTemplate.delete(key)
        }

        // when
        val futures = mutableListOf<CompletableFuture<Void>>()
        repeat(2) {
            val future = CompletableFuture.runAsync { atomicOperationService.increment(key, true) }
            futures.add(future)
        }

        futures.forEach { it.join() }

        // then
        redisTemplate.opsForValue().get(key) shouldBe 2
    }

    @DisplayName("2개의 스레드가 동시에 Redis에 접근하여 값을 증가시키더라도 Lua 스크립트에 의해 값이 각각 1씩 증가한다.")
    @Test
    fun increment_with_lua_script() {
        // given
        val key = "test"

        redisTemplate.opsForValue().get(key)?.let { redisTemplate.delete(key) }

        // when
        val futures = mutableListOf<CompletableFuture<Void>>()
        repeat(2) {
            val future = CompletableFuture.runAsync { atomicOperationService.incrementWithLuaScript(key) }
            futures.add(future)
        }

        futures.forEach { it.join() }

        // then
        redisTemplate.opsForValue().get(key) shouldBe 2
    }

    @DisplayName("2개의 스레드가 동시에 Redis에 접근하여 json 필드를 업데이트 하더라도 Lua 스크립트에 의해 하나씩 업데이트한다.")
    @Test
    fun whenUpdatingConcurrentlyThenBothUpdatesShouldBeApplied() {
        // given
        val key = "test1"
        val test1Dto = ConcurrencyTestDto(test1 = Test1Dto("test1"))
        val test2Dto = ConcurrencyTestDto(test2 = Test2Dto("test2"))

        // when
        val future1 =
            CompletableFuture.supplyAsync {
                atomicOperationService.updateJsonFieldWithLuaScript(key, "test1", test1Dto)
            }

        val future2 =
            CompletableFuture.supplyAsync {
                atomicOperationService.updateJsonFieldWithLuaScript(key, "test2", test2Dto)
            }

        val result1 = future1.get()
        val result2 = future2.get()

        // then
        val cmp =
            ConcurrencyTestDto(
                test1 = Test1Dto("test1"),
                test2 = Test2Dto("test2"),
            )

        (result1 == cmp || result2 == cmp) shouldBe true
    }
}
