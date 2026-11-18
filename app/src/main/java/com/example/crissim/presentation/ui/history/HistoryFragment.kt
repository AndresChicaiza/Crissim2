package com.example.crissim.presentation.ui.history

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.crissim.CrisisSimApp
import com.example.crissim.R
import com.example.crissim.domain.model.AttemptResult
import com.example.crissim.domain.model.Scenario
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class HistoryFragment : Fragment() {

    private var content: LinearLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val scroll = ScrollView(requireContext()).apply {
            setBackgroundColor(Color.rgb(247, 249, 252))
            clipToPadding = false
        }
        content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        scroll.addView(content)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = top)
            insets
        }
        return scroll
    }

    override fun onStart() {
        super.onStart()
        loadHistory()
    }

    override fun onDestroyView() {
        content = null
        super.onDestroyView()
    }

    private fun loadHistory() {
        val target = content ?: return
        target.removeAllViews()
        target.addView(text(getString(R.string.history_title), 26f, bold = true, color = 0xFF0F1D2F.toInt()))
        target.addView(text(getString(R.string.history_subtitle), 15f, color = 0xFF5A6B82.toInt(), top = 6))
        target.addView(text(getString(R.string.history_loading), 15f, color = 0xFF5A6B82.toInt(), top = 20))

        viewLifecycleOwner.lifecycleScope.launch {
            val container = CrisisSimApp.instance.container
            val data = withContext(Dispatchers.IO) {
                val user = container.userRepository.getActiveUser()
                if (user == null) return@withContext null
                val attempts = container.progressRepository.getAllAttemptHistory(user.id)
                val scenarios = container.scenarioRepository.getAllScenarios().associateBy { it.id }
                attempts to scenarios
            }

            val view = content ?: return@launch
            view.removeViews(2, (view.childCount - 2).coerceAtLeast(0))
            if (data == null) {
                view.addView(text(getString(R.string.history_no_user), 15f, color = 0xFF5A6B82.toInt(), top = 20))
                return@launch
            }

            val (attempts, scenarios) = data
            if (attempts.isEmpty()) {
                view.addView(text(getString(R.string.history_empty), 15f, color = 0xFF5A6B82.toInt(), top = 20))
                return@launch
            }

            view.addView(text(getString(R.string.history_attempt_count, attempts.size), 14f, bold = true, color = 0xFF185293.toInt(), top = 20))
            attempts.forEach { attempt ->
                view.addView(attemptCard(attempt, scenarios[attempt.scenarioId]))
            }
        }
    }

    private fun attemptCard(attempt: AttemptResult, scenario: Scenario?): View {
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.WHITE)
            strokeWidth = dp(1)
            setStrokeColor(Color.rgb(208, 215, 222))
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dp(12)
            layoutParams = params
        }
        val body = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        card.addView(body)

        val correctCount = attempt.decisions.count { it.isCorrect }
        val incorrectCount = attempt.decisions.size - correctCount
        val percent = if (attempt.maxScore > 0) attempt.score * 100 / attempt.maxScore else 0
        val title = scenario?.title?.get().orEmpty().ifBlank { attempt.scenarioId.uppercase() }
        body.addView(text(title, 18f, bold = true, color = 0xFF0F1D2F.toInt()))
        body.addView(text(
            getString(
                if (attempt.isPassed) R.string.history_passed else R.string.history_failed,
                attempt.scenarioId.uppercase()
            ),
            14f,
            bold = true,
            color = if (attempt.isPassed) 0xFF176343.toInt() else 0xFFC5221F.toInt(),
            top = 6
        ))
        val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(attempt.completedAt))
        body.addView(text(
            getString(
                R.string.history_attempt_summary,
                date,
                correctCount,
                attempt.decisions.size,
                incorrectCount,
                percent
            ),
            14f,
            color = 0xFF5A6B82.toInt(),
            top = 6
        ))

        attempt.decisions.forEachIndexed { index, decision ->
            val node = scenario?.getNode(decision.nodeId)
            val selected = node?.options?.find { it.id == decision.optionId }
            val recommended = node?.options?.firstOrNull { it.feedback?.isCorrect == true }
            val details = buildString {
                append(getString(R.string.history_decision_number, index + 1))
                if (!node?.narrative?.get().isNullOrBlank()) append("\n${node?.narrative?.get()}")
                append("\n")
                append(getString(R.string.history_your_answer, selected?.text?.get() ?: decision.optionId))
                append("\n")
                if (decision.isCorrect) {
                    append(getString(R.string.history_answer_correct))
                } else {
                    append(getString(R.string.history_answer_incorrect))
                    if (recommended != null) {
                        append("\n")
                        append(getString(R.string.history_recommended_answer, recommended.text.get()))
                    }
                }
                if (decision.wasTimeout) {
                    append("\n")
                    append(getString(R.string.history_timeout))
                }
            }
            body.addView(text(
                details,
                14f,
                color = if (decision.isCorrect) 0xFF176343.toInt() else 0xFFC5221F.toInt(),
                top = 14
            ))
        }
        return card
    }

    private fun text(
        value: String,
        size: Float,
        bold: Boolean = false,
        color: Int,
        top: Int = 0
    ) = TextView(requireContext()).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        gravity = Gravity.START
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = dp(top)
        layoutParams = params
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
