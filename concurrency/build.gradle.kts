plugins {
    id("spring-boot-convention")
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.redisson)
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.107.Final:osx-x86_64") // 인텔 맥 DNS 설정
}


configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "io.netty") {
                useVersion("4.1.107.Final")
            }
        }
    }
}
