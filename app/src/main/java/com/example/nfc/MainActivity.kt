package com.example.nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfc.ui.theme.NFCTheme
import java.security.MessageDigest
import java.util.*

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var showDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            NFCTheme {
                NFCReplicationScreen(
                    onNfcScan = { startNfcScan() },
                    showDialog = showDialog,
                    onDismissDialog = { showDialog = false }
                )
            }
        }
    }

    private fun startNfcScan() {
        showDialog = true
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                processNfcTag(it)
                showDialog = false
            }
        }
    }

    private fun processNfcTag(tag: Tag) {
        val isoDep = IsoDep.get(tag)
        isoDep?.use {
            it.connect()
            val command = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x00, 0x7F.toByte())
            val response = it.transceive(command)
            val isValid = verifySignature(response)
            runOnUiThread {
                showToast(if (isValid) "Carte authentique" else "Carte falsifiée")
            }
        }
    }

    private fun verifySignature(data: ByteArray): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return Arrays.equals(hash, getExpectedSignature())
    }

    private fun getExpectedSignature(): ByteArray {
        return byteArrayOf( /* Signature officielle du gouvernement */ )
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun NFCReplicationScreen(onNfcScan: () -> Unit, showDialog: Boolean, onDismissDialog: () -> Unit) {
    var recordName by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = Color.Gray) {
                Text(
                    text = "Nav Bar Bottom",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            TextField(
                value = recordName,
                onValueChange = { recordName = it },
                label = { Text("Nom de l'enregistrement") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onNfcScan,
                modifier = Modifier
                    .size(150.dp)
                    .background(Color.Blue, shape = CircleShape),
                shape = CircleShape
            ) {
                Text("🚀", fontSize = 36.sp, color = Color.White)
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Lecture NFC en cours") },
                text = { Text("Veuillez approcher la carte d'identité du lecteur NFC.") },
                confirmButton = {
                    Button(onClick = onDismissDialog) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NFCReplicationScreenPreview() {
    NFCTheme {
        NFCReplicationScreen(onNfcScan = {}, showDialog = false, onDismissDialog = {})
    }
}