package kr.co.taek.dev.redis.example.concurrency

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.transaction.annotation.EnableTransactionManagement

@EnableTransactionManagement
@SpringBootApplication
class RedisConcurrencyExpBootstrap

fun main(args: Array<String>) {
    runApplication<RedisConcurrencyExpBootstrap>(*args)
}
