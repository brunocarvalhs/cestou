package br.com.brunocarvalhs.howmuch.app.foundation.performance

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.util.SparseIntArray
import androidx.core.app.FrameMetricsAggregator
import androidx.core.util.size
import androidx.metrics.performance.JankStats
import br.com.brunocarvalhs.howmuch.BuildConfig
import br.com.brunocarvalhs.howmuch.app.foundation.constants.FIVE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.FOUR_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ONE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.SIX_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.THREE_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.TWO_INT
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ZERO_INT
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import java.util.concurrent.ConcurrentLinkedQueue

object PerformanceMonitor : Application.ActivityLifecycleCallbacks {

    private val firebasePerformance = FirebasePerformance.getInstance()
    private val monitors = mutableMapOf<Activity, ActivityPerformance>()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // No implementation needed
    }

    override fun onActivityStarted(activity: Activity) {
        val perf = ActivityPerformance(activity)
        monitors[activity] = perf
        perf.startMonitoring()
    }

    override fun onActivityResumed(activity: Activity) {
        // No implementation needed
    }

    override fun onActivityPaused(activity: Activity) {
        // No implementation needed
    }

    override fun onActivityStopped(activity: Activity) {
        monitors.remove(activity)?.stopMonitoring()
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // No implementation needed
    }

    override fun onActivityDestroyed(activity: Activity) {
        monitors.remove(activity)
    }

    private class ActivityPerformance(private val activity: Activity) {

        private var frameMetricsAggregator: FrameMetricsAggregator? = null
        private var jankStats: JankStats? = null
        private var trace: Trace? = null

        private val frameQueue = ConcurrentLinkedQueue<Double>()
        private val handler = android.os.Handler(Looper.getMainLooper())

        private val periodicReporter = object : Runnable {
            override fun run() {
                reportFrameSummary()
                handler.postDelayed(this, REPORT_INTERVAL_MS)
            }
        }

        fun startMonitoring() {
            startFirebaseTrace()
            frameMetricsAggregator = FrameMetricsAggregator(
                FrameMetricsAggregator.TOTAL_DURATION or
                    FrameMetricsAggregator.DRAW_DURATION or
                    FrameMetricsAggregator.INPUT_DURATION or
                    FrameMetricsAggregator.SYNC_DURATION or
                    FrameMetricsAggregator.SWAP_DURATION or
                    FrameMetricsAggregator.ANIMATION_DURATION or
                    FrameMetricsAggregator.LAYOUT_MEASURE_DURATION
            ).apply { add(activity) }
            jankStats = JankStats.createAndTrack(activity.window) { frameData ->
                val frameTimeMs = frameData.frameDurationUiNanos / FRAME
                frameQueue.add(frameTimeMs)
                if (frameTimeMs > JANK_THRESHOLD_MS && BuildConfig.DEBUG) {
                    Log.d("PerformanceMonitor", "JANK detected: $frameTimeMs ms")
                }
            }
            handler.postDelayed(periodicReporter, REPORT_INTERVAL_MS)
        }

        fun stopMonitoring() {
            stopJankStats()
            reportAndStopFrameMetrics()
            stopPeriodicReporting()
            stopFirebaseTrace()
        }

        private fun startFirebaseTrace() {
            trace = firebasePerformance.newTrace(
                "activity_performance_${activity.localClassName}"
            ).apply { start() }
        }

        private fun stopJankStats() {
            jankStats?.isTrackingEnabled = false
            jankStats = null
        }

        private fun stopPeriodicReporting() {
            handler.removeCallbacks(periodicReporter)
            frameQueue.clear()
        }

        private fun stopFirebaseTrace() {
            trace?.stop()
            trace = null
        }

        private fun reportAndStopFrameMetrics() {
            frameMetricsAggregator?.let { aggregator ->
                aggregator.metrics?.forEachIndexed { index, sparseArray ->
                    sparseArray?.let { array ->
                        val metricName = metricNameForIndex(index)
                        val metricsMap = arrayToReadableMap(array)
                        sendToFirebase(
                            "frame_metrics_${activity.localClassName}_$metricName",
                            metricsMap
                        )
                        if (BuildConfig.DEBUG) {
                            Log.d("PerformanceMonitor", "Metric: $metricName, Data: $metricsMap")
                        }
                    }
                }
                aggregator.remove(activity)
            }
            frameMetricsAggregator = null
        }

        private fun reportFrameSummary() {
            if (frameQueue.isEmpty()) return

            val frames = frameQueue.toList()
            frameQueue.clear()

            val average: Double = frames.average()
            val max: Double = frames.maxOrNull() ?: ZERO_FLOAT
            val p95: Double = frames.sorted().let { sorted ->
                sorted[(sorted.size * P95).toInt().coerceAtMost(sorted.size - ONE_INT)]
            }

            val metricsMap: Map<String, Any> = mapOf(
                "average_frame_ms" to average,
                "max_frame_ms" to max,
                "p95_frame_ms" to p95,
                "total_frames" to frames.size
            )

            sendToFirebase("frame_summary_${activity.localClassName}", metricsMap)
        }

        private fun metricNameForIndex(index: Int): String = when (index) {
            ZERO_INT -> "TOTAL_DURATION"
            ONE_INT -> "DRAW_DURATION"
            TWO_INT -> "INPUT_DURATION"
            THREE_INT -> "SYNC_DURATION"
            FOUR_INT -> "SWAP_DURATION"
            FIVE_INT -> "ANIMATION_DURATION"
            SIX_INT -> "LAYOUT_MEASURE_DURATION"
            else -> "UNKNOWN"
        }

        private fun arrayToReadableMap(array: SparseIntArray): Map<String, String> {
            val map = mutableMapOf<String, String>()
            for (i in ZERO_INT until array.size) {
                val timeMs = array.keyAt(i)
                val count = array.valueAt(i)
                map["${timeMs}ms"] = "$count frames"
            }
            return map
        }

        private fun sendToFirebase(eventName: String, params: Map<String, Any>) {
            trace?.apply {
                params.forEach { (key, value) ->
                    putAttribute(key, value.toString())
                }
            }
            if (BuildConfig.DEBUG) {
                Log.d("PerformanceMonitor", "Event: $eventName, Params: $params")
            }
        }

        companion object {
            private const val ZERO_FLOAT = 0.0
            private const val P95 = 0.95
            private const val FRAME = 1_000_000.0
            private const val REPORT_INTERVAL_MS = 1000L
            private const val JANK_THRESHOLD_MS = 16.0
        }
    }
}
