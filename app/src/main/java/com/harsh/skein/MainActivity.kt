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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.harsh.skein.ui.theme.SkeinTheme

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

    // 1. State: do we currently have READ_SMS? Checked once when the screen first appears.
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 2. Launcher: shows the system "Allow?" dialog and reports the result back.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // results is a Map<String, Boolean>: each permission -> was it granted?
        hasPermission = results[Manifest.permission.READ_SMS] == true
    }

    // 3. UI: status text + a button that fires the request.
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = if (hasPermission) "SMS permission: GRANTED ✅"
            else "SMS permission: NOT granted ❌"
        )
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
        var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }

        Button(enabled = hasPermission,
            onClick = { messages= readSmsMessages(context) }) {
            Text("get messages")
        }
        LazyColumn {
            items(messages) { message ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(message.displayName)
                    Text(message.body)
                }
            }
        }
    }
}

data class SmsMessage(
    val sender: String,        // raw number from the SMS store
    val body: String,
    val date: Long,
    val displayName: String    // contact name if known, else the number
)

// Reduce any phone number to a comparable form: digits only, last 10.
// "+91 98765-43210" and "9876543210" both become "9876543210".
fun normalizeNumber(number: String): String =
    number.filter { it.isDigit() }.takeLast(10)

// Read all contacts once, returning a map: normalizedNumber -> contactName.
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
        val nameColumn = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
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

    // Build the number -> name lookup once, up front.
    val contactNames = readContacts(context)

    // Which columns we want — like SELECT address, body, date
    val projection = arrayOf(
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE
    )

    // Run the query against the system SMS table, newest first.
    val cursor = context.contentResolver.query(
        Telephony.Sms.CONTENT_URI,    // FROM  (the system SMS table)
        projection,                   // SELECT columns
        null,                         // WHERE — none for now
        null,                         // WHERE arguments — none
        Telephony.Sms.DATE + " DESC"  // ORDER BY date, newest first
    )

    // use { } auto-closes the cursor when we're done (even if something throws).
    cursor?.use { c ->
        val senderColumn = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyColumn = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val dateColumn = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

        // Walk each row of the result.
        while (c.moveToNext()) {
            val sender = c.getString(senderColumn) ?: "Unknown"
            val body = c.getString(bodyColumn) ?: ""
            val date = c.getLong(dateColumn)
            // Look up the name; fall back to the raw number if not a contact.
            val displayName = contactNames[normalizeNumber(sender)] ?: sender
            messages.add(SmsMessage(sender, body, date, displayName))
        }
    }

    return messages
}