package com.example.alarmkata

import android.Manifest
import android.app.*
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.util.Calendar

object Scheduler {
    private fun pi(c: Context) = PendingIntent.getBroadcast(
        c, 0, Intent(c, AlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun schedule(c: Context) {
        val sp = c.getSharedPreferences("p", 0)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, sp.getInt("h", 7))
            set(Calendar.MINUTE, sp.getInt("m", 0))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val show = PendingIntent.getActivity(
            c, 1, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        c.getSystemService(AlarmManager::class.java)
            .setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, show), pi(c))
    }

    fun cancel(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(pi(c))
    }
}

class MainActivity : Activity() {
    private val sp by lazy { getSharedPreferences("p", 0) }
    private lateinit var soundLabel: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO, "android.permission.POST_NOTIFICATIONS"), 2
        )

        val title = TextView(this).apply {
            text = "NCV Alarm"
            textSize = 28f
            setPadding(0, 0, 0, 24)
        }
        val tp = TimePicker(this).apply {
            setIs24HourView(true)
            hour = sp.getInt("h", 7)
            minute = sp.getInt("m", 0)
        }
        val kw = EditText(this).apply {
            hint = "Kata untuk mematikan alarm"
            setText(sp.getString("kw", "bangun"))
        }
        val rep = CheckBox(this).apply {
            text = "Ulangi setiap hari"
            isChecked = sp.getBoolean("repeat", false)
        }
        soundLabel = TextView(this)
        showSound()

        val pick = Button(this).apply {
            text = "Pilih nada alarm"
            setOnClickListener {
                startActivityForResult(
                    Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("audio/*")
                        .addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        ), 1
                )
            }
        }
        val save = Button(this).apply {
            text = "Simpan & aktifkan"
            setOnClickListener {
                val word = kw.text.toString().trim().lowercase()
                if (word.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Isi kata dulu", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                sp.edit().putInt("h", tp.hour).putInt("m", tp.minute)
                    .putString("kw", word).putBoolean("repeat", rep.isChecked).apply()
                Scheduler.schedule(this@MainActivity)
                Toast.makeText(
                    this@MainActivity,
                    "Alarm aktif %02d:%02d".format(tp.hour, tp.minute),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        val cancel = Button(this).apply {
            text = "Matikan alarm terjadwal"
            setOnClickListener {
                Scheduler.cancel(this@MainActivity)
                Toast.makeText(this@MainActivity, "Alarm dibatalkan", Toast.LENGTH_SHORT).show()
            }
        }
        val credit = TextView(this).apply {
            text = "Created by NCV"
            textSize = 14f
            alpha = 0.6f
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 16)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(title)
            addView(tp); addView(kw); addView(rep)
            addView(pick); addView(soundLabel); addView(save); addView(cancel)
            addView(credit)
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun showSound() {
        soundLabel.text = "Nada: " +
            (sp.getString("sound", null)?.let { Uri.parse(it).lastPathSegment } ?: "bawaan")
    }

    override fun onActivityResult(rc: Int, res: Int, d: Intent?) {
        super.onActivityResult(rc, res, d)
        val u = d?.data
        if (rc == 1 && res == RESULT_OK && u != null) {
            contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            sp.edit().putString("sound", u.toString()).apply()
            showSound()
        }
    }
}
