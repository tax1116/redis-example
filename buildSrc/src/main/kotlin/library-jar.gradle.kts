import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByName

tasks.getByName<Jar>("jar") {
    enabled = true
}
