package com.harsh.skein

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.harsh.skein.ui.theme.SkeinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkeinTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SmsPermissionScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SmsPermissionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results[Manifest.permission.READ_SMS] == true
    }
    var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }

    Column(modifier = modifier.padding(16.dp)) {
        if (hasPermission) {
            LaunchedEffect(Unit) {
                messages = withContext(Dispatchers.IO) {
                    readSmsMessages(context)
                }
            }

            LazyColumn {
                items(messages) { message ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(message.displayName)
                        Text(message.body)
                    }
                }
            }
        } else Text("SMS permission: NOT granted ❌")

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS
                )
            )
        }) {
            Text("Request permissions")
        }

//        Button(enabled = hasPermission,
//            onClick = { messages = readSmsMessages(context) }) {
//            Text("get messages")
//        }
    }
}

data class SmsMessage(
    val sender: String,
    val body: String,
    val date: Long,
    val displayName: String
)

// Digits only, last 10 — so differently formatted numbers still match.
fun normalizeNumber(number: String): String =
    number.filter { it.isDigit() }.takeLast(10)

fun readContacts(context: Context): Map<String, String> {
    val names = mutableMapOf<String, String>()

    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )

    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        null
    )?.use { c ->
        val nameColumn =
            c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberColumn = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

        while (c.moveToNext()) {
            val name = c.getString(nameColumn) ?: continue
            val number = c.getString(numberColumn) ?: continue
            names[normalizeNumber(number)] = name
        }
    }

    return names
}

fun readSmsMessages(context: Context): List<SmsMessage> {
    val messages = mutableListOf<SmsMessage>()
    val contactNames = readContacts(context)

    val projection = arrayOf(
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE
    )

    val cursor = context.contentResolver.query(
        Telephony.Sms.CONTENT_URI,
        projection,
        null,
        null,
        Telephony.Sms.DATE + " DESC"
    )

    cursor?.use { c ->
        val senderColumn = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyColumn = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val dateColumn = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

        while (c.moveToNext()) {
            val sender = c.getString(senderColumn) ?: "Unknown"
            val body = c.getString(bodyColumn) ?: ""
            val date = c.getLong(dateColumn)
            val displayName = contactNames[normalizeNumber(sender)] ?: sender
            messages.add(SmsMessage(sender, body, date, displayName))
        }
    }

    return messages
}
