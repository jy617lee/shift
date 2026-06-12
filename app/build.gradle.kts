plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    jacoco
}

android {
    namespace = "com.schedule.shift"
    compileSdk = 36

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
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
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

private val coverageExcludes =
    listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "**/*ComposableSingletons*",
        "**/ui/theme/**",
        "**/di/**",
        "**/*_Factory*",
        "**/*_HiltComponents*",
        "**/*Hilt*",
    )

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports { html.required = true }

    sourceDirectories.setFrom(files("src/main/kotlin", "src/main/java"))
    classDirectories.setFrom(
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(coverageExcludes)
        },
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
    )
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")

    sourceDirectories.setFrom(files("src/main/kotlin", "src/main/java"))
    classDirectories.setFrom(
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(coverageExcludes)
        },
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
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
    notCompatibleWithConfigurationCache("파일 시스템 직접 읽기 — 빌드 설정 캐시 미지원")

    doLast {
        fun lines(dir: String) =
            fileTree(dir) { include("**/*.kt") }
                .sumOf { f -> f.readLines().count { it.isNotBlank() } }

        val src = lines("src/main")
        val test = lines("src/test") + lines("src/androidTest")

        if (src < 300) {
            logger.lifecycle("프로덕션 코드 300줄 미만 — 비율 검사 건너뜀")
            return@doLast
        }

        val pct = test * 100 / src
        logger.lifecycle("테스트 비율: 프로덕션 ${src}줄 / 테스트 ${test}줄 = $pct%")

        if (pct < 50) {
            throw GradleException("테스트 코드 부족 ($pct% < 50%). 테스트를 추가해주세요.")
        }
    }
}

// ── Dependencies ─────────────────────────────────────────────────────────────

dependencies {
    implementation(project(":ui"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":ocr"))
    implementation(project(":analytics"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.work.runtime.ktx)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(libs.mlkit.text.recognition.korean)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.work.testing)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
