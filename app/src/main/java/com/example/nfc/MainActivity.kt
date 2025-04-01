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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
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
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var selectedScreen by remember { mutableStateOf("Accueil") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedScreen == "Accueil",
                    onClick = { selectedScreen = "Accueil" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Accueil") },
                    label = { Text("Accueil") }
                )
                NavigationBarItem(
                    selected = selectedScreen == "Enregistrement",
                    onClick = { selectedScreen = "Enregistrement" },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Enregistrement") },
                    label = { Text("Enregistrement") }
                )
                NavigationBarItem(
                    selected = selectedScreen == "Informations",
                    onClick = { selectedScreen = "Informations" },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Informations") },
                    label = { Text("Informations") }
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
            verticalArrangement = Arrangement.Center
        ) {
            when (selectedScreen) {
                "Accueil" -> NFCReplicationScreen()
                "Enregistrement" -> RegistrationScreen()
                "Informations" -> InformationScreen()
            }
        }
    }
}

@Composable
fun NFCReplicationScreen() {
    var recordName by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier.fillMaxSize(),
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
            onClick = { /* Logique de scan NFC */ },
            modifier = Modifier
                .size(150.dp)
                .background(Color.Blue, shape = CircleShape),
            shape = CircleShape
        ) {
            Text("🚀", fontSize = 36.sp, color = Color.White)
        }
    }
}

@Composable
fun RegistrationScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Liste des pièces d'identité")
        Text("1 - Validé")
        Text("2 - Non validé")
    }
}

@Composable
fun InformationScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Cette application permet de scanner et vérifier l'authenticité des pièces d'identité via NFC.")
    }
}

@Preview(showBackground = true)
@Composable
fun AppNavigationPreview() {
    NFCTheme {
        AppNavigation()
    }
}
