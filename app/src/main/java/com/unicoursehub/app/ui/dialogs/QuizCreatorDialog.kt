package com.unicoursehub.app.ui.dialogs

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.unicoursehub.app.R
import com.unicoursehub.app.data.QuizQuestion

class QuizCreatorDialog(
    private val context: Context,
    existingQuestions: List<QuizQuestion>,
    private val onSave: (List<QuizQuestion>) -> Unit
) {
    private val questions = existingQuestions.toMutableList()

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_quiz_creator, null)
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

        val tvQuizCount = dialogView.findViewById<TextView>(R.id.tvQuizCount)
        val containerQuestions = dialogView.findViewById<LinearLayout>(R.id.containerQuestions)
        val etQuestionText = dialogView.findViewById<EditText>(R.id.etQuestionText)
        val etOptionA = dialogView.findViewById<EditText>(R.id.etOptionA)
        val etOptionB = dialogView.findViewById<EditText>(R.id.etOptionB)
        val etOptionC = dialogView.findViewById<EditText>(R.id.etOptionC)
        val etOptionD = dialogView.findViewById<EditText>(R.id.etOptionD)
        val rgCorrectOption = dialogView.findViewById<RadioGroup>(R.id.rgCorrectOption)
        val rbOptionA = dialogView.findViewById<RadioButton>(R.id.rbOptionA)
        val rbOptionB = dialogView.findViewById<RadioButton>(R.id.rbOptionB)
        val rbOptionC = dialogView.findViewById<RadioButton>(R.id.rbOptionC)
        val rbOptionD = dialogView.findViewById<RadioButton>(R.id.rbOptionD)
        val btnAddQuestion = dialogView.findViewById<Button>(R.id.btnAddQuestion)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSaveQuiz = dialogView.findViewById<Button>(R.id.btnSaveQuiz)

        fun refreshList() {
            tvQuizCount.text = "${questions.size} Questions Added"
            containerQuestions.removeAllViews()

            for ((index, q) in questions.withIndex()) {
                val qView = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(R.drawable.bg_card_white)
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 16)
                    layoutParams = params
                }

                val titleTv = TextView(context).apply {
                    text = "Q${index + 1}: ${q.questionText}"
                    setTextColor(context.getColor(R.color.text_primary))
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }

                val optionsTv = TextView(context).apply {
                    val correctLetter = when (q.correctOption) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        else -> "D"
                    }
                    text = "A) ${q.optionA}\nB) ${q.optionB}\nC) ${q.optionC}\nD) ${q.optionD}\nCorrect: Option $correctLetter"
                    setTextColor(context.getColor(R.color.text_secondary))
                    textSize = 12f
                    setPadding(0, 6, 0, 6)
                }

                val deleteBtn = Button(context, null, 0, R.style.ButtonOutline).apply {
                    text = "Delete Question"
                    setTextColor(context.getColor(R.color.danger))
                    setOnClickListener {
                        questions.removeAt(index)
                        refreshList()
                    }
                }

                qView.addView(titleTv)
                qView.addView(optionsTv)
                qView.addView(deleteBtn)
                containerQuestions.addView(qView)
            }
        }

        refreshList()

        btnAddQuestion.setOnClickListener {
            val qText = etQuestionText.text.toString().trim()
            val optA = etOptionA.text.toString().trim()
            val optB = etOptionB.text.toString().trim()
            val optC = etOptionC.text.toString().trim()
            val optD = etOptionD.text.toString().trim()

            if (qText.isEmpty() || optA.isEmpty() || optB.isEmpty() || optC.isEmpty() || optD.isEmpty()) {
                Toast.makeText(context, "Please fill in question and all 4 options", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val correctIdx = when (rgCorrectOption.checkedRadioButtonId) {
                R.id.rbOptionB -> 1
                R.id.rbOptionC -> 2
                R.id.rbOptionD -> 3
                else -> 0
            }

            questions.add(
                QuizQuestion(
                    questionText = qText,
                    optionA = optA,
                    optionB = optB,
                    optionC = optC,
                    optionD = optD,
                    correctOption = correctIdx
                )
            )

            etQuestionText.setText("")
            etOptionA.setText("")
            etOptionB.setText("")
            etOptionC.setText("")
            etOptionD.setText("")
            rbOptionA.isChecked = true

            Toast.makeText(context, "Question added", Toast.LENGTH_SHORT).show()
            refreshList()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSaveQuiz.setOnClickListener {
            onSave(questions)
            Toast.makeText(context, "Quiz saved (${questions.size} questions)", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}
