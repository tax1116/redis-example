package kr.co.taek.dev.redis.example.concurrency.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class LettuceConfig {
    @Bean
    fun redisTemplate(): RedisTemplate<String, Int> {
        return RedisTemplate<String, Int>().apply {
            connectionFactory = lettuceConnectionFactory()
            keySerializer = StringRedisSerializer()
            valueSerializer = GenericJackson2JsonRedisSerializer()
            setEnableTransactionSupport(true)
        }
    }

    @Profile("!test")
    @Primary
    @Bean
    fun lettuceConnectionFactory(): LettuceConnectionFactory {
        return LettuceConnectionFactory("localhost", 6379)
    }
}
