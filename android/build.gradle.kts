plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}

// 최초 1회 실행: ./gradlew installGitHooks
tasks.register("installGitHooks") {
    group = "setup"
    description = "pre-push 훅을 .git/hooks 에 설치합니다."
    notCompatibleWithConfigurationCache("파일 시스템 직접 접근 — 일회성 설정 태스크")
    doLast {
        val hook = rootProject.file(".git/hooks/pre-push")
        hook.parentFile.mkdirs()
        hook.writeText("#!/bin/sh\n\"$(git rev-parse --show-toplevel)/scripts/pre-push.sh\"\n")
        hook.setExecutable(true)
        logger.lifecycle("✅ pre-push 훅 설치 완료: ${hook.absolutePath}")
    }
}
