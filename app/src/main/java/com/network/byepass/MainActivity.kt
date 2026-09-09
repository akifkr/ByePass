package com.network.byepass

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.network.byepass.service.TunnelService

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startTunnelService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F1115)
                ) {
                    val running by TunnelService.isRunning.collectAsState()
                    val logList by TunnelService.logs.collectAsState()
                    val listState = rememberLazyListState()

                    LaunchedEffect(logList.size) {
                        if (logList.isNotEmpty()) {
                            listState.animateScrollToItem(logList.size - 1)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ByePass",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "DPI Circumvention Engine",
                                    fontSize = 13.sp,
                                    color = Color(0xFF888E9B)
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (running) Color(0xFF1B382B) else Color(0xFF2E1C1F)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = if (running) "AKTİF" else "KAPALI",
                                    color = if (running) Color(0xFF4CAF50) else Color(0xFFE57373),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (running) {
                                    val stopIntent = Intent(this@MainActivity, TunnelService::class.java).apply {
                                        action = TunnelService.ACTION_DISCONNECT
                                    }
                                    startService(stopIntent)
                                } else {
                                    val prep = VpnService.prepare(this@MainActivity)
                                    if (prep != null) {
                                        vpnPermissionLauncher.launch(prep)
                                    } else {
                                        startTunnelService()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (running) Color(0xFFD32F2F) else Color(0xFF1976D2)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = if (running) "Bağlantıyı Kes" else "Bypass Başlat",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Ağ Günlüğü",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA0A5B1)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF161920), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            LazyColumn(state = listState) {
                                items(logList) { entry ->
                                    Text(
                                        text = "> $entry",
                                        color = Color(0xFF81C784),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startTunnelService() {
        val intent = Intent(this, TunnelService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}