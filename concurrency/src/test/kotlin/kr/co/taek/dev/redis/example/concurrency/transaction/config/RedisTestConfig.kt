package kr.co.taek.dev.redis.example.concurrency.transaction.config

import com.redis.testcontainers.RedisContainer
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.testcontainers.utility.DockerImageName

@Profile("test")
@Configuration
class RedisTestConfig {
    @Bean(initMethod = "start", destroyMethod = "stop")
    fun redisContainer(): RedisContainer {
        return RedisContainer(DockerImageName.parse("redis:latest"))
    }

    @Bean
    @DependsOn("redisContainer")
    fun redissonClient(redisContainer: RedisContainer): RedissonClient {
        val host = redisContainer.host
        val port = redisContainer.firstMappedPort
        val config =
            Config().apply {
                this.useSingleServer().address = "redis://$host:$port"
            }
        return Redisson.create(config)
    }

    @Bean
    @DependsOn("redisContainer")
    fun lettuceConnectionFactory(redisContainer: RedisContainer): LettuceConnectionFactory {
        return LettuceConnectionFactory(redisContainer.host, redisContainer.firstMappedPort)
    }
}
