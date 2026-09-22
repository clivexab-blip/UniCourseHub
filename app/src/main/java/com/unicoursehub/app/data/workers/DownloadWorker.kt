package com.unicoursehub.app.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseProvider
import com.unicoursehub.app.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val videoUrl = inputData.getString("video_url") ?: return@withContext Result.failure()
        val lessonId = inputData.getLong("lesson_id", -1L)
        if (lessonId == -1L) return@withContext Result.failure()

        val notificationHelper = NotificationHelper(context)
        val initialNotification = notificationHelper.showDownloadNotification(
            applicationContext.getString(R.string.msg_downloading),
            applicationContext.getString(R.string.msg_starting_download),
            0
        )
        setForeground(ForegroundInfo(1, initialNotification))

        try {
            val fileName = "video_lesson_$lessonId.mp4"
            val file = File(context.filesDir, fileName)

            val url = URL(videoUrl)
            val connection = url.openConnection()
            connection.connect()
            val fileLength = connection.contentLength

            url.openStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var total: Long = 0
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        total += bytesRead
                        if (fileLength > 0) {
                            val progress = (total * 100 / fileLength).toInt()
                            // Update notification less frequently to avoid spam
                            if (progress % 10 == 0) {
                                val updatedNotification = notificationHelper.showDownloadNotification(
                                    applicationContext.getString(R.string.downloading_video),
                                    applicationContext.getString(R.string.download_progress_format, progress),
                                    progress
                                )
                                setForeground(ForegroundInfo(1, updatedNotification))
                            }
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            // Update database using DatabaseHelper
            val db = com.unicoursehub.app.data.DatabaseHelper.getInstance(context)
            db.updateLocalVideoPath(lessonId, file.absolutePath)

            notificationHelper.showDownloadNotification(
                applicationContext.getString(R.string.msg_download_complete),
                applicationContext.getString(R.string.msg_video_saved_offline),
                100
            )

            Result.success(workDataOf("local_path" to file.absolutePath))
        } catch (e: Exception) {
            notificationHelper.showDownloadNotification(
                applicationContext.getString(R.string.msg_download_failed),
                e.message ?: applicationContext.getString(R.string.msg_unknown_error)
            )
            Result.failure()
        }
    }
}
