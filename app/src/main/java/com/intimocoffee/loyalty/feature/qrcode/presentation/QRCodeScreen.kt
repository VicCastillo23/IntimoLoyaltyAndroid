package com.intimocoffee.loyalty.feature.qrcode.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.intimocoffee.loyalty.core.datastore.SessionDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QRCodeViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore
) : ViewModel() {
    
    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap
    
    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName
    
    init { generateQR() }
    
    private fun generateQR() {
        viewModelScope.launch {
            val customerId = sessionDataStore.customerId.first() ?: return@launch
            _customerName.value = sessionDataStore.customerName.first() ?: ""
            val phone = sessionDataStore.customerPhone.first()?.trim().orEmpty()
            val qrData = if (phone.isNotBlank()) {
                "INTIMO_LOYALTY:$phone"
            } else {
                "INTIMO_LOYALTY:$customerId"
            }
            
            try {
                val writer = QRCodeWriter()
                val size = 512
                val bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, size, size)
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
                _qrBitmap.value = bitmap
            } catch (_: Exception) { }
        }
    }
}

@Composable
fun QRCodeScreen(viewModel: QRCodeViewModel = hiltViewModel()) {
    val bitmap by viewModel.qrBitmap.collectAsState()
    val name by viewModel.customerName.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tu Código QR", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            "Muestra este código al cajero para acumular puntos",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color(0xFF888888)
        )
        
        Spacer(Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.size(280.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                bitmap?.let {
                    Image(bitmap = it.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.fillMaxSize())
                } ?: CircularProgressIndicator(color = Color.Black)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Intimo Coffee Loyalty", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF888888))
    }
}
