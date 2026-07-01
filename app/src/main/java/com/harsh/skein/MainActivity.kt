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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.harsh.skein.ui.theme.AvatarColors
import com.harsh.skein.ui.theme.AvatarInitialColor
import com.harsh.skein.ui.theme.LocalSkeinColors
import com.harsh.skein.ui.theme.SkeinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkeinTheme {
                SkeinApp()
            }
        }
    }
}

@Composable
fun SkeinApp() {
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

    if (!hasPermission) {
        PermissionScreen(
            onRequest = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.READ_CONTACTS
                    )
                )
            }
        )
        return
    }

    var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    LaunchedEffect(Unit) {
        messages = withContext(Dispatchers.IO) { readSmsMessages(context) }
    }

    val conversations = remember(messages) { groupIntoConversations(messages) }

    var openThreadId by remember { mutableStateOf<Long?>(null) }
    val openConversation = conversations.find { it.threadId == openThreadId }

    if (openConversation == null) {
        ConversationListScreen(
            conversations = conversations,
            onOpen = { openThreadId = it }
        )
    } else {
        ThreadScreen(
            conversation = openConversation,
            onBack = { openThreadId = null }
        )
    }
}

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    val skein = LocalSkeinColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(skein.bg)
            .padding(24.dp)
    ) {
        Text(
            text = "Skein",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = skein.text,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Text(
            text = "Skein needs permission to read your messages and contacts.",
            fontSize = 15.sp,
            color = skein.sub
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequest) {
            Text("Grant permissions")
        }
    }
}

@Composable
fun ConversationListScreen(
    conversations: List<Conversation>,
    onOpen: (Long) -> Unit
) {
    val skein = LocalSkeinColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skein.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Skein",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = skein.text,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(skein.header),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔍", fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AvatarColors[0])
                )
            }

            FilterChips()
            Spacer(modifier = Modifier.height(12.dp))

            // List sheet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(skein.surface)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { SectionLabel("RECENT") }
                    items(conversations) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            onClick = { onOpen(conversation.threadId) }
                        )
                    }
                }
            }
        }

        // Compose FAB (visual only for now — sending is Phase 1)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(skein.accent),
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 30.sp, color = Color.White)
        }
    }
}

@Composable
fun FilterChips() {
    val labels = listOf("All", "Unread", "Family", "Work")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(label = label, active = index == 0)
        }
    }
}

@Composable
fun FilterChip(label: String, active: Boolean) {
    val skein = LocalSkeinColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) skein.accent else skein.header)
            .padding(horizontal = 15.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color.White else skein.text
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    val skein = LocalSkeinColors.current
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        color = skein.sub,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
fun SkeinAvatar(name: String, size: Int = 48) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(avatarColorFor(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            color = AvatarInitialColor,
            fontSize = (size / 2.6f).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    val skein = LocalSkeinColors.current
    val lastMessage = conversation.messages.first()
    Column(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeinAvatar(conversation.contactName)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.contactName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = skein.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatTime(lastMessage.date),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = skein.sub
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = lastMessage.body,
                    fontSize = 13.5.sp,
                    color = skein.sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(
            color = skein.line,
            modifier = Modifier.padding(start = 76.dp)
        )
    }
}

@Composable
fun ThreadScreen(conversation: Conversation, onBack: () -> Unit) {
    val skein = LocalSkeinColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(skein.bg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(skein.header)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("‹", fontSize = 24.sp, color = skein.text)
            }
            SkeinAvatar(conversation.contactName, size = 42)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = conversation.contactName,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = skein.text
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(conversation.messages.sortedBy { it.date }) { message ->
                MessageBubble(message)
            }
        }
    }
}

@Composable
fun MessageBubble(message: SmsMessage) {
    val skein = LocalSkeinColors.current
    val isSent = message.type == Telephony.Sms.MESSAGE_TYPE_SENT
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSent) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    if (isSent) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
                    else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
                )
                .background(if (isSent) skein.bubbleOut else skein.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.body,
                fontSize = 14.sp,
                fontWeight = if (isSent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSent) skein.onBubbleOut else skein.text
            )
        }
        Text(
            text = formatTime(message.date),
            fontSize = 11.sp,
            color = skein.sub,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

data class SmsMessage(
    val threadId: Long,
    val sender: String,
    val body: String,
    val date: Long,
    val displayName: String,
    val type: Int
)

data class Conversation(
    val threadId: Long,
    val contactName: String,
    val messages: List<SmsMessage>
)

// Group all messages by the system's thread id, newest conversation first.
fun groupIntoConversations(messages: List<SmsMessage>): List<Conversation> =
    messages
        .groupBy { it.threadId }
        .map { (threadId, threadMessages) ->
            Conversation(
                threadId = threadId,
                contactName = threadMessages.first().displayName,
                messages = threadMessages
            )
        }
        .sortedByDescending { it.messages.first().date }

// Pick a stable avatar color for a name.
fun avatarColorFor(name: String): Color =
    AvatarColors[Math.floorMod(name.hashCode(), AvatarColors.size)]

// Millis since epoch -> "Jul 1, 9:05 PM"
fun formatTime(millis: Long): String {
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}

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
        Telephony.Sms.THREAD_ID,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
        Telephony.Sms.TYPE
    )

    val cursor = context.contentResolver.query(
        Telephony.Sms.CONTENT_URI,
        projection,
        null,
        null,
        Telephony.Sms.DATE + " DESC"
    )

    cursor?.use { c ->
        val threadIdColumn = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
        val senderColumn = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyColumn = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val dateColumn = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
        val typeColumn = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)

        while (c.moveToNext()) {
            val threadId = c.getLong(threadIdColumn)
            val sender = c.getString(senderColumn) ?: "Unknown"
            val body = c.getString(bodyColumn) ?: ""
            val date = c.getLong(dateColumn)
            val displayName = contactNames[normalizeNumber(sender)] ?: sender
            val type = c.getInt(typeColumn)
            messages.add(SmsMessage(threadId, sender, body, date, displayName, type))
        }
    }

    return messages
}
