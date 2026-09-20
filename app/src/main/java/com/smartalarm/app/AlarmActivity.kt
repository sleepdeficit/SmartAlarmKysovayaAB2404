package com.smartalarm.app

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartalarm.app.service.AlarmService
import com.smartalarm.app.service.SmartNotificationListenerService
import com.smartalarm.app.ui.theme.SmartAlarmTheme
import com.smartalarm.app.util.AlarmScheduler
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// экран который открывается при звонке поверх экрана блокировки
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        unlockFlags()
        super.onCreate(savedInstanceState)

        val lbl = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: ""
        val math = intent.getBooleanExtra(AlarmScheduler.EXTRA_ALARM_MATH, false)

        setContent {
            SmartAlarmTheme(darkTheme = true) {
                AlarmUi(
                    label = lbl,
                    needMath = math,
                    onDismiss = {
                        val i = Intent(this, AlarmService::class.java).apply {
                            action = AlarmService.ACTION_DISMISS_ALARM
                        }
                        startService(i)
                        finish()
                    },
                    onSnooze = {
                        val i = Intent(this, AlarmService::class.java).apply {
                            action = AlarmService.ACTION_SNOOZE_ALARM
                        }
                        startService(i)
                        finish()
                    }
                )
            }
        }
    }

    // чтоб экран загорался и не требовал пароль
    private fun unlockFlags() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                km.requestDismissKeyguard(this, null)
            }
        } catch (e: Exception) {}

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    override fun onBackPressed() {}
}

@Composable
fun AlarmUi(
    label: String,
    needMath: Boolean,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var curTime by remember { mutableStateOf(getNowTime()) }
    var curDate by remember { mutableStateOf(getNowDate()) }

    LaunchedEffect(Unit) {
        while (true) {
            curTime = getNowTime()
            curDate = getNowDate()
            delay(1000L)
        }
    }

    val notifsList by SmartNotificationListenerService.capturedNotifications.collectAsState()

    var solved by remember { mutableStateOf(!needMath) }
    val problem = remember { makeProblem() }
    var err by remember { mutableStateOf(false) }

    val trans = rememberInfiniteTransition(label = "anim")
    val sc by trans.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13101C))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(80.dp)
                    .scale(sc)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = curTime,
                fontSize = 62.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = curDate,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )

            if (label.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // задача по математике если включена
            if (needMath && !solved) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF231E33))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Решите пример для отключения:",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${problem.a} ${problem.op} ${problem.b} = ?",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Yellow
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            problem.opts.take(2).forEach { num ->
                                OutlinedButton(
                                    onClick = {
                                        if (num == problem.ans) {
                                            solved = true
                                            err = false
                                        } else {
                                            err = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("$num", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            problem.opts.drop(2).take(2).forEach { num ->
                                OutlinedButton(
                                    onClick = {
                                        if (num == problem.ans) {
                                            solved = true
                                            err = false
                                        } else {
                                            err = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("$num", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (err) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Неправильный ответ", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                if (notifsList.isNotEmpty()) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1B1728))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Пока вы спали (${notifsList.size}):", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(notifsList.take(3)) { n ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF262038),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(n.appName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            if (n.title.isNotBlank()) Text(n.title, fontSize = 13.sp, color = Color.White)
                                            if (n.text.isNotBlank()) Text(n.text, fontSize = 12.sp, color = Color.LightGray, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Snooze, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Отложить 10м")
                }

                Button(
                    onClick = {
                        if (solved) onDismiss()
                    },
                    enabled = solved,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (solved) MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (solved) "Выключить" else "Реши пример")
                }
            }
        }
    }
}

private fun getNowTime(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}

private fun getNowDate(): String {
    val f = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
    return f.format(Date()).replaceFirstChar { it.uppercase() }
}

data class ProblemData(
    val a: Int,
    val b: Int,
    val op: String,
    val ans: Int,
    val opts: List<Int>
)

// генерация примера
private fun makeProblem(): ProblemData {
    val plus = Random.nextBoolean()
    var a = 0
    var b = 0
    var op = "+"
    var ans = 0

    if (plus) {
        a = Random.nextInt(10, 40)
        b = Random.nextInt(5, 30)
        op = "+"
        ans = a + b
    } else {
        a = Random.nextInt(20, 60)
        b = Random.nextInt(5, 19)
        op = "-"
        ans = a - b
    }

    val list = ArrayList<Int>()
    list.add(ans)
    var attempts = 0
    while (list.size < 4 && attempts < 50) {
        attempts++
        val rand = ans + Random.nextInt(-9, 10)
        if (rand != ans && !list.contains(rand)) {
            list.add(rand)
        }
    }
    while (list.size < 4) {
        list.add(ans + list.size + 1)
    }
    list.shuffle()

    return ProblemData(a, b, op, ans, list)
}
