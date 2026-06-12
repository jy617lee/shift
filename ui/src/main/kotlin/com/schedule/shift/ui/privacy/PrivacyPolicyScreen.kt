package com.schedule.shift.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = "개인정보처리방침",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        },
    ) { padding ->
        PrivacyPolicyContent(modifier = Modifier.padding(padding))
    }
}

@Suppress("LongMethod")
@Composable
internal fun PrivacyPolicyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        PrivacySectionTitle("수집하는 정보")
        PrivacyItem(
            title = "익명 UUID",
            purpose = "서비스 이용 분석",
            retention = "서비스 이용 기간",
        )
        PrivacyItem(
            title = "스케쥴표 이미지 (이름·사번 포함 가능)",
            purpose = "OCR 인식 품질 개선",
            retention = "사용자 동의 시에만 수집, 30일 후 자동 파기",
        )
        PrivacyItem(
            title = "이벤트 로그 (화면 이동, 기능 사용 기록)",
            purpose = "서비스 개선",
            retention = "180일 후 집계 데이터만 유지",
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrivacySectionTitle("수집하지 않는 정보")
        Text(
            text = "광고 ID, 위치 정보, 계정 정보(이름·이메일·전화번호)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrivacySectionTitle("데이터 보관 원칙")
        Text(
            text = "스케쥴 데이터는 기기 내부에만 저장되며 외부 서버로 전송되지 않습니다. " +
                "앱을 삭제하면 모든 데이터가 영구 삭제되며 복구할 수 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrivacySectionTitle("문의")
        Text(
            text = "개인정보 관련 문의는 앱 스토어 리뷰 또는 GitHub 이슈로 접수해주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun PrivacySectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Suppress("LongParameterList")
@Composable
private fun PrivacyItem(title: String, purpose: String, retention: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "목적: $purpose",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "보관: $retention",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
