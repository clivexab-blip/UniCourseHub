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

        // SPEED OPTIMIZATION: Check Local Cache first
        val cached = getCachedInsight(todayDate)
        if (cached != null) {
            parseAndDisplayResponse(cached)
            return
        }

        if (isInternetAvailable()) {
            generateDailyInsight(todayDate)
        } else {
            showFallbackWisdom("Connection Required", "waiting for internet connection")
        }
    }

    private fun generateDailyInsight(todayKey: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvInsightContent.text = "Consulting the senior developers..."

        val prompt = "Provide one specific, high-impact career or coding insight for a junior Android developer today. " +
                "Focus on what makes someone a 'wonderful coder' and professional best practices. " +
                "Also provide two short pro-tips. " +
                "Use PLAIN TEXT ONLY. DO NOT use Markdown symbols like #, *, or **. " +
                "Format response exactly: Title: [Title]\nContent: [Content]\nTip1: [Tip1]\nTip2: [Tip2]"

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                AiHelper.callGemini(prompt)
            }
            
            binding.progressBar.visibility = View.GONE
            when (result) {
                is AiHelper.AiResult.Success -> {
                    saveInsightToCache(todayKey, result.text)
                    parseAndDisplayResponse(result.text)
                }
                is AiHelper.AiResult.Error -> {
                    showFallbackWisdom("Vault Notice", "AI Sync in progress. Loading local senior wisdom...")
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
            // Lines are already scrubbed by AiHelper
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

    private fun showFallbackWisdom(title: String? = null, content: String? = null) {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val localInsights = listOf(
            Insight("The Power of Empathy", "A wonderful coder writes for people, not just machines. Empathy for the next maintainer is the ultimate best practice."),
            Insight("Fundamentals are Forever", "Frameworks change, but fundamentals like Data Structures and Algorithms stay. Sharpen them daily."),
            Insight("The Art of Refactoring", "Deleting code is as important as writing it. Less code often means fewer bugs. Leave it cleaner than you found it.")
        )
        
        val daily = localInsights[dayOfYear % localInsights.size]
        binding.tvInsightTitle.text = title ?: daily.title
        binding.tvInsightContent.text = content ?: daily.content
        
        binding.tipsContainer.removeAllViews()
        addTip("Clean code is readable code.")
        addTip("Always test your edge cases.")
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

    data class Insight(val title: String, val content: String)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
