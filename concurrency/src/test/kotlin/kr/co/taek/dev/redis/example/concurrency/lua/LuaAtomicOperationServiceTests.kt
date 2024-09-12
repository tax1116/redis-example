package kr.co.taek.dev.redis.example.concurrency.lua

import io.kotest.matchers.shouldBe
import kr.co.taek.dev.redis.example.concurrency.lua.dto.ConcurrencyTestDto
import kr.co.taek.dev.redis.example.concurrency.lua.dto.Test1Dto
import kr.co.taek.dev.redis.example.concurrency.lua.dto.Test2Dto
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CompletableFuture

@ActiveProfiles("test")
@SpringBootTest
class LuaAtomicOperationServiceTests {
    @Autowired
    private lateinit var luaAtomicOperationService: LuaAtomicOperationService

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, Int>

    @DisplayName("2개의 스레드가 동시에 Redis에 접근하여 값을 증가시키더라도 Lua 스크립트에 의해 값이 각각 1씩 증가한다.")
    @Test
    fun increment_with_lua_script() {
        // given
        val key = "test"

        redisTemplate.opsForValue().get(key)?.let { redisTemplate.delete(key) }

        // when
        val futures = mutableListOf<CompletableFuture<Void>>()
        repeat(2) {
            val future = CompletableFuture.runAsync { luaAtomicOperationService.incrementWithLuaScript(key) }
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
                luaAtomicOperationService.updateJsonFieldWithLuaScript(key, "test1", test1Dto)
            }

        val future2 =
            CompletableFuture.supplyAsync {
                luaAtomicOperationService.updateJsonFieldWithLuaScript(key, "test2", test2Dto)
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
