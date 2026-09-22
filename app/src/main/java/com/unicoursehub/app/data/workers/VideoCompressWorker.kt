package com.unicoursehub.app.data.workers

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

class VideoCompressWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @OptIn(UnstableApi::class)
    override suspend fun doWork(): Result {
        val inputPath = inputData.getString("input_path") ?: return Result.failure()
        val lessonId = inputData.getLong("lesson_id", -1)
        val projectId = inputData.getLong("project_id", -1)
        val lessonTitle = inputData.getString("lesson_title") ?: "Video"
        
        val db = DatabaseHelper.getInstance(context)
        val notificationHelper = NotificationHelper(context)
        
        val notification = notificationHelper.createCompressionNotification(
            applicationContext.getString(R.string.compressing_format, lessonTitle), 
            applicationContext.getString(R.string.msg_processing_video)
        )
        setForeground(ForegroundInfo(2, notification))

        val outputPath = File(context.filesDir, "compressed_${System.currentTimeMillis()}.mp4").absolutePath
        
        return withContext(Dispatchers.Main) {
            coroutineScope {
                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .build()

                val mediaItem = MediaItem.fromUri(inputPath)
                val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()

                suspendCancellableCoroutine { continuation ->
                    val listener = object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            notificationHelper.showCompressionNotification(
                                applicationContext.getString(R.string.compression_done),
                                applicationContext.getString(R.string.compression_ready_format, lessonTitle),
                                100
                            )
                            if (lessonId != -1L) {
                                db.updateLessonStatus(lessonId, "published")
                                db.updateLocalVideoPath(lessonId, outputPath)
                            } else if (projectId != -1L) {
                                db.updateProjectVideoPath(projectId, outputPath)
                            }
                            if (continuation.isActive) continuation.resume(Result.success())
                        }

                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            notificationHelper.showCompressionNotification(
                                applicationContext.getString(R.string.msg_compression_failed),
                                exportException.message ?: applicationContext.getString(R.string.msg_unknown_error)
                            )
                            if (lessonId != -1L) {
                                db.updateLessonStatus(lessonId, "error")
                            }
                            if (continuation.isActive) continuation.resume(Result.failure())
                        }
                    }
                    
                    transformer.addListener(listener)
                    
                    val progressHolder = androidx.media3.transformer.ProgressHolder()
                    val progressJob = this.launch {
                        while (continuation.isActive) {
                            val state = transformer.getProgress(progressHolder)
                            if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                                notificationHelper.showCompressionNotification(
                                    applicationContext.getString(R.string.compressing_format, lessonTitle),
                                    applicationContext.getString(R.string.msg_processing_progress, progressHolder.progress),
                                    progressHolder.progress
                                )
                            }
                            delay(1000)
                        }
                    }

                    try {
                        transformer.start(editedMediaItem, outputPath)
                    } catch (e: Exception) {
                        progressJob.cancel()
                        if (lessonId != -1L) {
                            db.updateLessonStatus(lessonId, "error")
                        }
                        if (continuation.isActive) continuation.resume(Result.failure())
                    }

                    continuation.invokeOnCancellation {
                        progressJob.cancel()
                        transformer.cancel()
                    }
                }
            }
        }
    }
}
