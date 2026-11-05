package com.unicoursehub.app.ui.dialogs

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.unicoursehub.app.R
import com.unicoursehub.app.data.QuizQuestion

class QuizStudentDialog(
    private val context: Context,
    private val questions: List<QuizQuestion>
) {
    private var currentIndex = 0
    private var score = 0

    fun show() {
        if (questions.isEmpty()) return

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_quiz_student, null)
        val dialog = AlertDialog.Builder(context, R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()

        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window.setGravity(Gravity.CENTER)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = window.attributes.apply {
                dimAmount = 0.75f
            }
        }

        val tvProgress = dialogView.findViewById<TextView>(R.id.tvProgress)
        val layoutQuestion = dialogView.findViewById<LinearLayout>(R.id.layoutQuestion)
        val tvQuestionText = dialogView.findViewById<TextView>(R.id.tvQuestionText)
        val btnOptionA = dialogView.findViewById<Button>(R.id.btnOptionA)
        val btnOptionB = dialogView.findViewById<Button>(R.id.btnOptionB)
        val btnOptionC = dialogView.findViewById<Button>(R.id.btnOptionC)
        val btnOptionD = dialogView.findViewById<Button>(R.id.btnOptionD)
        val btnNextQuestion = dialogView.findViewById<Button>(R.id.btnNextQuestion)

        val layoutResult = dialogView.findViewById<LinearLayout>(R.id.layoutResult)
        val tvScore = dialogView.findViewById<TextView>(R.id.tvScore)
        val tvFeedback = dialogView.findViewById<TextView>(R.id.tvFeedback)
        val btnRetryQuiz = dialogView.findViewById<Button>(R.id.btnRetryQuiz)
        val btnCloseQuiz = dialogView.findViewById<Button>(R.id.btnCloseQuiz)

        val optionButtons = listOf(btnOptionA, btnOptionB, btnOptionC, btnOptionD)

        fun resetOptionButton(btn: Button, text: String) {
            btn.text = text
            btn.isEnabled = true
            btn.setTextColor(context.getColor(R.color.text_primary))
            btn.backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.surface))
        }

        fun highlightCorrect(btn: Button, text: String, suffix: String = "  ✓ Correct") {
            btn.text = text + suffix
            btn.setTextColor(Color.parseColor("#065F46"))
            btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D1FAE5"))
        }

        fun highlightWrong(btn: Button, text: String) {
            btn.text = text + "  ✗ Incorrect"
            btn.setTextColor(Color.parseColor("#991B1B"))
            btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
        }

        fun loadQuestion() {
            val q = questions[currentIndex]
            tvProgress.text = "Question ${currentIndex + 1} of ${questions.size}"
            tvQuestionText.text = q.questionText
            btnNextQuestion.visibility = View.GONE

            val rawTexts = listOf(
                "A. ${q.optionA}",
                "B. ${q.optionB}",
                "C. ${q.optionC}",
                "D. ${q.optionD}"
            )

            for (i in 0..3) {
                resetOptionButton(optionButtons[i], rawTexts[i])
                optionButtons[i].setOnClickListener {
                    // Disable all buttons once selected
                    optionButtons.forEach { b -> b.isEnabled = false }

                    if (i == q.correctOption) {
                        score++
                        highlightCorrect(optionButtons[i], rawTexts[i])
                    } else {
                        highlightWrong(optionButtons[i], rawTexts[i])
                        // Also highlight the correct answer in green
                        highlightCorrect(optionButtons[q.correctOption], rawTexts[q.correctOption], "  ✓ Correct Answer")
                    }

                    btnNextQuestion.text = if (currentIndex + 1 < questions.size) "Next Question" else "See Final Score"
                    btnNextQuestion.visibility = View.VISIBLE
                }
            }
        }

        fun showResults() {
            layoutQuestion.visibility = View.GONE
            layoutResult.visibility = View.VISIBLE
            val percent = (score.toFloat() / questions.size * 100).toInt()
            tvScore.text = "Your Score: $score / ${questions.size} ($percent%)"

            tvFeedback.text = when {
                percent == 100 -> "Outstanding! Perfect score! 🌟"
                percent >= 50 -> "Good job! Keep learning! 👍"
                else -> "Keep reviewing the lesson content and try again! 💪"
            }
        }

        btnNextQuestion.setOnClickListener {
            if (currentIndex + 1 < questions.size) {
                currentIndex++
                loadQuestion()
            } else {
                showResults()
            }
        }

        btnRetryQuiz.setOnClickListener {
            currentIndex = 0
            score = 0
            layoutResult.visibility = View.GONE
            layoutQuestion.visibility = View.VISIBLE
            loadQuestion()
        }

        btnCloseQuiz.setOnClickListener {
            dialog.dismiss()
        }

        loadQuestion()
        dialog.show()
    }
}
