package com.example.crissim.presentation.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.example.crissim.R
import com.example.crissim.databinding.ItemOnboardingPageBinding

/**
 * Adapter para el ViewPager2 del onboarding.
 */
class OnboardingAdapter(
    private val pages: List<OnboardingPage>,
    private val onSkipClick: () -> Unit,
    private val onNextClick: (Int) -> Unit,
    private val onFinishClick: () -> Unit
) : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemOnboardingPageBinding) : RecyclerView.ViewHolder(binding.root)

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(@NonNull holder: ViewHolder, position: Int) {
        val page = pages[position]
        val isLastPage = position == pages.size - 1

        holder.binding.ivOnboardingImage.setImageResource(page.imageRes)
        holder.binding.tvOnboardingTitle.setText(page.titleRes)
        holder.binding.tvOnboardingDescription.setText(page.descriptionRes)

        // Botón "Omitir" - visible en todas excepto la última
        holder.binding.btnOnboardingSkip.visibility = if (isLastPage) View.GONE else View.VISIBLE
        holder.binding.btnOnboardingSkip.setOnClickListener { onSkipClick() }

        // Botón "Continuar" / "Finalizar"
        val nextTextRes = if (isLastPage) R.string.onboarding_finish else R.string.onboarding_next
        holder.binding.btnOnboardingNext.setText(nextTextRes)
        holder.binding.btnOnboardingNext.setOnClickListener {
            if (isLastPage) {
                onFinishClick()
            } else {
                onNextClick(position + 1)
            }
        }
    }

    override fun getItemCount(): Int = pages.size
}

/**
 * Modelo de una página del onboarding.
 */
data class OnboardingPage(
    val imageRes: Int,
    val titleRes: Int,
    val descriptionRes: Int
)