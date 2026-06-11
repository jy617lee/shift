plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    jacoco
}

android {
    namespace = "com.schedule.shift"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.schedule.shift"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    lint {
        lintConfig = file("$rootDir/config/lint/lint.xml")
        abortOnError = true
        htmlOutput = file("${layout.buildDirectory.get()}/reports/lint/lint-report.html")
    }
}

detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    source.setFrom("src/main/kotlin", "src/main/java")
}

ktlint {
    version.set("1.3.1")
    android.set(true)
    outputToConsole.set(true)
    filter { exclude("**/generated/**") }
}

// ── JaCoCo ──────────────────────────────────────────────────────────────────

jacoco {
    toolVersion = "0.8.12"
}

private val coverageExcludes = listOf(
    "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "**/*Test*.*", "**/*ComposableSingletons*", "**/ui/theme/**"
)

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports { html.required = true }

    sourceDirectories.setFrom(files("src/main/kotlin", "src/main/java"))
    classDirectories.setFrom(
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(coverageExcludes)
        }
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        }
    )
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")

    sourceDirectories.setFrom(files("src/main/kotlin", "src/main/java"))
    classDirectories.setFrom(
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(coverageExcludes)
        }
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        }
    )

    violationRules {
        rule {
            limit {
                // Phase 1 시작 기준. 코드가 쌓이면 점진적으로 올릴 것.
                minimum = "0.30".toBigDecimal()
            }
        }
    }
}

// ── 테스트-프로덕션 비율 ────────────────────────────────────────────────────

tasks.register("checkTestRatio") {
    group = "verification"
    description = "테스트 코드가 프로덕션 대비 50% 이상인지 검사합니다."

    doLast {
        fun lines(dir: String) = fileTree(dir) { include("**/*.kt") }
            .sumOf { f -> f.readLines().count { it.isNotBlank() } }

        val src = lines("src/main")
        val test = lines("src/test") + lines("src/androidTest")

        if (src < 100) {
            logger.lifecycle("프로덕션 코드 100줄 미만 — 비율 검사 건너뜀")
            return@doLast
        }

        val pct = test * 100 / src
        logger.lifecycle("테스트 비율: 프로덕션 ${src}줄 / 테스트 ${test}줄 = ${pct}%")

        if (pct < 50) {
            throw GradleException("테스트 코드 부족 (${pct}% < 50%). 테스트를 추가해주세요.")
        }
    }
}

// ── Dependencies ─────────────────────────────────────────────────────────────

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
