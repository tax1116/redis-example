package kr.co.taek.dev.redis.example.concurrency

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RedisConcurrencyExpBootstrap

fun main(args: Array<String>) {
    runApplication<RedisConcurrencyExpBootstrap>(*args)
}
