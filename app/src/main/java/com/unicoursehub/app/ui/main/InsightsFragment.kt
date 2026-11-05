package com.unicoursehub.app.ui.main

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.unicoursehub.app.R
import com.unicoursehub.app.databinding.FragmentInsightsBinding
import com.unicoursehub.app.util.AiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val displayDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        binding.tvDate.text = displayDate.uppercase()

        // 1. Check if an insight for today was already loaded and saved
        val cached = getCachedInsight(todayDate)
        if (cached != null) {
            // Already loaded for today! Display instantly with no waiting
            binding.progressBar.visibility = View.GONE
            parseAndDisplayResponse(cached)
            return
        }

        // 2. First time opening today: Show fallback wisdom immediately
        val localWisdomFormatted = getLocalDailyWisdomFormatted()
        parseAndDisplayResponse(localWisdomFormatted)
        binding.progressBar.visibility = View.GONE

        // 3. Asynchronously fetch AI insight for today in background if internet is available
        if (isInternetAvailable()) {
            fetchBackgroundAiInsight(todayDate)
        } else {
            // Save local wisdom if offline so it stays saved for today
            saveInsightToCache(todayDate, localWisdomFormatted)
        }
    }

    private fun fetchBackgroundAiInsight(todayKey: String) {
        val prompt = "Provide one specific, high-impact career or coding insight for a junior Android developer today. " +
                "Focus on what makes someone a 'wonderful coder' and professional best practices. " +
                "Also provide two short pro-tips. " +
                "Use PLAIN TEXT ONLY. DO NOT use Markdown symbols like #, *, or **. " +
                "Format response exactly: Title: [Title]\nContent: [Content]\nTip1: [Tip1]\nTip2: [Tip2]"

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                AiHelper.callGemini(prompt)
            }
            
            if (result is AiHelper.AiResult.Success) {
                // Save AI response to cache for today and replace fallback content seamlessly
                saveInsightToCache(todayKey, result.text)
                if (_binding != null) {
                    parseAndDisplayResponse(result.text)
                }
            }
        }
    }

    private fun parseAndDisplayResponse(text: String) {
        val lines = text.split("\n")
        var title = "Senior Wisdom"
        var content = ""
        val tips = mutableListOf<String>()

        lines.forEach { line ->
            val cleanLine = line.trim()
            when {
                cleanLine.startsWith("Title:", ignoreCase = true) -> title = cleanLine.removePrefix("Title:").trim()
                cleanLine.startsWith("Content:", ignoreCase = true) -> content = cleanLine.removePrefix("Content:").trim()
                cleanLine.startsWith("Tip1:", ignoreCase = true) -> tips.add(cleanLine.removePrefix("Tip1:").trim())
                cleanLine.startsWith("Tip2:", ignoreCase = true) -> tips.add(cleanLine.removePrefix("Tip2:").trim())
            }
        }

        if (content.isEmpty()) content = text.replace("*", "").trim()

        binding.tvInsightTitle.text = title
        binding.tvInsightContent.text = content

        binding.tipsContainer.removeAllViews()
        if (tips.isNotEmpty()) {
            tips.forEach { addTip(it) }
        } else {
            addTip("Consistency is the mark of a pro.")
            addTip("Always verify your data inputs.")
        }
    }

    private fun getLocalDailyWisdomFormatted(): String {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val localInsights = listOf(
            Insight("The Power of Empathy", "A wonderful coder writes for people, not just machines. Empathy for the next maintainer is the ultimate best practice.", listOf("Clean code is readable code.", "Always test your edge cases.")),
            Insight("Fundamentals are Forever", "Frameworks change, but fundamentals like Data Structures and Algorithms stay. Sharpen them daily.", listOf("Don't repeat yourself (DRY principle).", "Keep it simple, stupid (KISS).")),
            Insight("The Art of Refactoring", "Deleting code is as important as writing it. Less code often means fewer bugs. Leave it cleaner than you found it.", listOf("Consistency is key in a codebase.", "Write code for the next maintainer.")),
            Insight("Mastering Asynchronous Code", "Understanding threads, coroutines, and reactive streams makes your app smooth and responsive under load.", listOf("Never block the main UI thread.", "Handle cancellation gracefully.")),
            Insight("Design Patterns Matter", "Use architecture patterns like MVVM/MVI to keep your codebase clean, modular, and easy to test.", listOf("Separate UI from business logic.", "Keep view models lean and focused."))
        )
        
        val daily = localInsights[dayOfYear % localInsights.size]
        return "Title: ${daily.title}\nContent: ${daily.content}\nTip1: ${daily.tips[0]}\nTip2: ${daily.tips[1]}"
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun addTip(text: String) {
        val tipView = LayoutInflater.from(requireContext()).inflate(R.layout.item_pro_tip, binding.tipsContainer, false)
        tipView.findViewById<TextView>(R.id.tvTipText).text = text
        binding.tipsContainer.addView(tipView)
        
        val params = tipView.layoutParams as LinearLayout.LayoutParams
        params.setMargins(0, 0, 0, 16)
        tipView.layoutParams = params
    }

    // CACHING LOGIC
    private fun getCachedInsight(date: String): String? {
        val pref = requireContext().getSharedPreferences("ai_cache", Context.MODE_PRIVATE)
        return if (pref.getString("insight_date", "") == date) {
            pref.getString("insight_text", null)
        } else null
    }

    private fun saveInsightToCache(date: String, text: String) {
        requireContext().getSharedPreferences("ai_cache", Context.MODE_PRIVATE).edit()
            .putString("insight_date", date)
            .putString("insight_text", text)
            .apply()
    }

    data class Insight(val title: String, val content: String, val tips: List<String>)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
