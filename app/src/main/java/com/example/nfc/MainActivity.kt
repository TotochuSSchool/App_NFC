package com.example.nfc

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NFCTheme {
                NFCReplicationScreen()
            }
        }
    }
}

@Composable
fun NFCReplicationScreen() {
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
                onClick = { /* TODO: Implémenter la diffusion NFC */ },
                modifier = Modifier
                    .size(150.dp)
                    .background(Color.Blue, shape = CircleShape),
                shape = CircleShape
            ) {
                Text("🚀", fontSize = 36.sp, color = Color.White)
            }

            Button(
                onClick = { /* TODO: Implémenter le changement d'enregistrement */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Switch Enregistrement")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NFCReplicationScreenPreview() {
    NFCTheme {
        NFCReplicationScreen()
    }
}
