package com.example.adhdblockscheduler.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adhdblockscheduler.model.DailyStats
import com.example.adhdblockscheduler.ui.viewmodel.SchedulerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: SchedulerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 실제로는 별도의 StatsViewModel을 두는 것이 좋지만, 
    // 여기서는 간단히 SchedulerViewModel이나 Repository를 통해 데이터를 가져온다고 가정합니다.
    // 현재는 uiState에 통계 데이터가 없으므로 샘플 데이터를 표시하거나 
    // 추후 ViewModel에 추가할 예정입니다.

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("집중 통계") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("오늘 총 집중 시간", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "0분", // 추후 실데이터 연결
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "최근 7일 기록",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 샘플 리스트 (데이터 연결 전)
            Text(
                text = "아직 기록이 없습니다. 집중 블록을 완료해 보세요!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
