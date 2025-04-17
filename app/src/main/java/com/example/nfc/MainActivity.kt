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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfc.ui.theme.NFCTheme
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // États pour l'UI
    var showDialog by mutableStateOf(false)
    var statusMessage by mutableStateOf("Attente de lecture NFC...")
    var computedFingerprint by mutableStateOf<String?>(null)
    var printedKey by mutableStateOf(TextFieldValue(""))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC non supporté sur cet appareil.", Toast.LENGTH_LONG).show()
            finish()
        }
        setContent {
            NFCTheme {
                NFCReplicationScreen(
                    onNfcScan = { startNfcScan() },
                    onVerify = { verifyPrintedKey() },
                    printedKey = printedKey,
                    onPrintedKeyChange = { printedKey = it },
                    statusMessage = statusMessage,
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
        isoDep?.use { dep ->
            try {
                dep.connect()
                val debugLog = StringBuilder()
                // --- 1. Lecture du certificat
                val selectCertCommand = byteArrayOf(
                    0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0x01, 0x02
                )
                val selectCertResponse = sendApduCommand(dep, selectCertCommand)
                debugLog.append("Réponse SELECT (certificat): ${selectCertResponse?.toHexString() ?: "Aucune réponse"}\n")
                if (!isResponseSuccess(selectCertResponse)) {
                    runOnUiThread {
                        statusMessage = "Échec de la commande SELECT (certificat).\n$debugLog"
                    }
                    return
                }
                val readCertCommand = byteArrayOf(
                    0x00, 0xB0.toByte(), 0x00, 0x00, 0x00
                )
                val readCertResponse = sendApduCommand(dep, readCertCommand)
                debugLog.append("Réponse READ BINARY (certificat): ${readCertResponse?.toHexString() ?: "Aucune réponse"}\n")
                if (!isResponseSuccess(readCertResponse)) {
                    runOnUiThread {
                        statusMessage = "Échec de la commande READ BINARY (certificat).\n$debugLog"
                    }
                    return
                }
                val certBytes = readCertResponse!!.copyOf(readCertResponse.size - 2)
                val cardCert = parseCertificate(certBytes)
                if (cardCert == null) {
                    runOnUiThread {
                        statusMessage = "Erreur lors du parsing du certificat.\n$debugLog"
                    }
                    return
                }
                // --- 2. Vérification du certificat avec le CSCA
                if (!verifyCertificate(cardCert)) {
                    runOnUiThread {
                        statusMessage = "La vérification du certificat a échoué.\n$debugLog"
                    }
                    return
                }
                // --- 3. Calcul de l’empreinte de la clé publique
                val fingerprint = computePublicKeyFingerprint(cardCert)
                computedFingerprint = fingerprint
                debugLog.append("Empreinte calculée: $fingerprint\n")
                // --- 4. Lecture des données personnelles (état‑civil, etc.)
                val selectDataCommand = byteArrayOf(
                    0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0x02, 0x03
                )
                val selectDataResponse = sendApduCommand(dep, selectDataCommand)
                debugLog.append("Réponse SELECT (données perso): ${selectDataResponse?.toHexString() ?: "Aucune réponse"}\n")
                if (!isResponseSuccess(selectDataResponse)) {
                    runOnUiThread {
                        statusMessage = "Échec de la commande SELECT (données perso).\n$debugLog"
                    }
                    return
                }
                val readDataCommand = byteArrayOf(
                    0x00, 0xB0.toByte(), 0x00, 0x00, 0x00
                )
                val readDataResponse = sendApduCommand(dep, readDataCommand)
                debugLog.append("Réponse READ BINARY (données perso): ${readDataResponse?.toHexString() ?: "Aucune réponse"}\n")
                if (!isResponseSuccess(readDataResponse)) {
                    runOnUiThread {
                        statusMessage = "Échec de la commande READ BINARY (données perso).\n$debugLog"
                    }
                    return
                }
                val personalDataBytes = readDataResponse!!.copyOf(readDataResponse.size - 2)
                val personalData = try {
                    String(personalDataBytes, Charsets.UTF_8)
                } catch (e: Exception) {
                    null
                }
                val debugInfo = debugLog.toString()
                runOnUiThread {
                    statusMessage = "Certificat vérifié.\nEmpreinte lue : $fingerprint\nDonnées perso: ${personalData ?: "Erreur"}\n\nLog APDU:\n$debugInfo\n\nEntrez la clé imprimée et appuyez sur 'Vérifier'."
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    statusMessage = "Erreur de communication NFC."
                }
            }
        }
    }

    
    private fun sendApduCommand(isoDep: IsoDep, command: ByteArray): ByteArray? {
        return try {
            isoDep.transceive(command)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    
    private fun isResponseSuccess(response: ByteArray?): Boolean {
        return response != null && response.size >= 2 &&
                response[response.size - 2] == 0x90.toByte() &&
                response[response.size - 1] == 0x00.toByte()
    }

    
    private fun ByteArray.toHexString(): String =
        joinToString(separator = " ") { String.format("%02X", it) }

    
    private fun parseCertificate(certBytes: ByteArray): X509Certificate? {
        return try {
            val cf = CertificateFactory.getInstance("X.509")
            val bais = ByteArrayInputStream(certBytes)
            cf.generateCertificate(bais) as X509Certificate
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    
    private fun verifyCertificate(cert: X509Certificate): Boolean {
        return try {
            val cscaCert = loadCSCACertificate()
            cert.verify(cscaCert.publicKey)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    
    private fun loadCSCACertificate(): X509Certificate {
        val inputStream = resources.openRawResource(R.raw.csca)
        val cf = CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(inputStream) as X509Certificate
    }

    
    private fun computePublicKeyFingerprint(cert: X509Certificate): String {
        return try {
            val pubKeyEncoded = cert.publicKey.encoded
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(pubKeyEncoded)
            digest.joinToString(separator = "") { String.format("%02X", it) }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    
    private fun verifyPrintedKey() {
        val expected = computedFingerprint?.uppercase(Locale.getDefault()) ?: ""
        val printed = printedKey.text.trim().toString().uppercase(Locale.getDefault())
        statusMessage = if (expected == printed && expected.isNotEmpty()) {
            "Authentification réussie : la clé correspond."
        } else {
            "Authentification échouée : la clé ne correspond pas."
        }
    }
}

@Composable
fun NFCReplicationScreen(
    onNfcScan: () -> Unit,
    onVerify: () -> Unit,
    printedKey: TextFieldValue,
    onPrintedKeyChange: (TextFieldValue) -> Unit,
    statusMessage: String,
    showDialog: Boolean,
    onDismissDialog: () -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = Color.Gray) {
                Text(
                    text = "Barre de navigation",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
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
            Text(
                text = statusMessage,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = printedKey,
                onValueChange = onPrintedKeyChange,
                label = { Text("Clé imprimée sur la carte") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onNfcScan,
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.Blue, shape = CircleShape),
                    shape = CircleShape
                ) {
                    Text("🚀", fontSize = 36.sp, color = Color.White)
                }
                Button(
                    onClick = onVerify,
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.Green, shape = CircleShape),
                    shape = CircleShape
                ) {
                    Text("Vérifier", fontSize = 24.sp, color = Color.White)
                }
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
        NFCReplicationScreen(
            onNfcScan = {},
            onVerify = {},
            printedKey = TextFieldValue(""),
            onPrintedKeyChange = {},
            statusMessage = "Attente de lecture NFC...",
            showDialog = false,
            onDismissDialog = {}
        )
    }
}
