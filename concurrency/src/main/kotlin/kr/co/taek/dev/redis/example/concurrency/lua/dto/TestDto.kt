package kr.co.taek.dev.redis.example.concurrency.lua.dto

data class ConcurrencyTestDto(
    val test1: Test1Dto? = null,
    val test2: Test2Dto? = null,
    val test3: Test3Dto? = null,
    val test4: Test4Dto? = null,
)

data class Test1Dto(
    val value: String = "",
)

data class Test2Dto(
    val value: String = "",
)

data class Test3Dto(
    val value: String = "",
)

data class Test4Dto(
    val value: String = "",
)
