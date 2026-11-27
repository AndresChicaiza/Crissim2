package com.example.crissim.presentation.ui.certificates

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.crissim.CrisisSimApp
import com.example.crissim.R
import com.example.crissim.data.local.entity.CertificateEntity
import com.example.crissim.databinding.FragmentCertificatesBinding
import com.example.crissim.databinding.ItemCertificateBinding
import com.example.crissim.domain.model.Certificate
import com.example.crissim.domain.model.Level
import com.example.crissim.domain.model.VerificationResult
import com.example.crissim.domain.usecase.GenerateCertificatePdfUseCase
import com.example.crissim.domain.usecase.VerifyCertificateUseCase
import com.example.crissim.domain.usecase.VerifyPdfFileUseCase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de Certificados Digitales y Verificación (HU-18, HU-19).
 * Lista certificados, genera PDFs y permite verificar firma QR y Hash SHA-256 (100% offline).
 */
class CertificatesFragment : Fragment() {

    private var _binding: FragmentCertificatesBinding? = null
    private val binding get() = _binding!!

    private val generatePdfUseCase: GenerateCertificatePdfUseCase by lazy {
        val container = CrisisSimApp.instance.container
        GenerateCertificatePdfUseCase(container.certificateRepository)
    }

    private val verifyCertificateUseCase: VerifyCertificateUseCase by lazy {
        val container = CrisisSimApp.instance.container
        VerifyCertificateUseCase(container.certificateRepository)
    }

    private val verifyPdfFileUseCase: VerifyPdfFileUseCase by lazy {
        val db = CrisisSimApp.instance.database
        VerifyPdfFileUseCase(db.certificateDao(), db.certificateFileDao())
    }

    private var activeVerifyDialogView: View? = null

    private val selectPdfLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && activeVerifyDialogView != null) {
            lifecycleScope.launch {
                val startTime = System.currentTimeMillis()
                val result = verifyPdfFileUseCase.execute(requireContext(), uri)
                val elapsed = System.currentTimeMillis() - startTime
                renderVerificationResult(activeVerifyDialogView!!, result, elapsed)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCertificatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvCertificates.layoutManager = LinearLayoutManager(requireContext())
        setupButtons()
        loadCertificates()
    }

    private fun setupButtons() {
        binding.btnVerifyCertificate.setOnClickListener {
            showVerifyDialog()
        }
    }

    private fun loadCertificates() {
        lifecycleScope.launch {
            val userRepo = CrisisSimApp.instance.container.userRepository
            val activeUser = userRepo.getActiveUser()
            if (activeUser == null) {
                binding.tvEmpty.isVisible = true
                binding.rvCertificates.isVisible = false
                binding.btnVerifyCertificate.isVisible = false
                return@launch
            }

            // Restricción por Rol: la verificación es exclusiva para Docentes, Brigadistas y Administrativos (no Estudiantes)
            val canVerify = activeUser.role != com.example.crissim.domain.model.UserRole.STUDENT
            binding.btnVerifyCertificate.isVisible = canVerify

            val certDao = CrisisSimApp.instance.database.certificateDao()
            val certs = withContext(Dispatchers.IO) {
                certDao.getCertificatesByUser(activeUser.id)
            }

            if (certs.isEmpty()) {
                binding.tvEmpty.isVisible = true
                binding.rvCertificates.isVisible = false
            } else {
                binding.tvEmpty.isVisible = false
                binding.rvCertificates.isVisible = true
                val currentFragment = this@CertificatesFragment
                binding.rvCertificates.adapter = CertificateAdapter(
                    items = certs,
                    lifecycleScope = lifecycleScope,
                    fragment = currentFragment,
                    generatePdfUseCase = generatePdfUseCase,
                    snackbarView = binding.root,
                    onVerifyItem = { item ->
                        if (canVerify) {
                            showVerifyDialog(item)
                        } else {
                            val targetView = activity?.findViewById<View>(android.R.id.content) ?: binding.root
                            Snackbar.make(
                                targetView,
                                getString(R.string.certs_verify_restricted),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
        }
    }

    private fun showVerifyDialog(presetCert: CertificateEntity? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_verify_certificate, null)
        activeVerifyDialogView = dialogView

        val etInput = dialogView.findViewById<EditText>(R.id.et_qr_input)
        val btnVerifyQr = dialogView.findViewById<View>(R.id.btn_verify_qr)
        val btnSelectPdf = dialogView.findViewById<View>(R.id.btn_select_pdf)
        val btnClose = dialogView.findViewById<View>(R.id.btn_dialog_verify_close)

        if (presetCert != null) {
            val domainCert = Certificate(
                id = presetCert.id,
                userId = presetCert.userId,
                userName = presetCert.userName,
                level = Level.valueOf(presetCert.level),
                scenarioId = presetCert.scenarioId,
                score = presetCert.score,
                maxScore = presetCert.maxScore,
                issuedAt = presetCert.issuedAt,
                signature = presetCert.signature,
                certificateUuid = presetCert.certificateUuid
            )
            etInput?.setText(domainCert.buildQrPayload())
        }

        btnVerifyQr?.setOnClickListener {
            val input = etInput?.text?.toString()?.trim().orEmpty()
            if (input.isEmpty()) {
                Snackbar.make(dialogView, "Ingresa un código QR o firma digital para verificar", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val startTime = System.currentTimeMillis()
                val result = verifyCertificateUseCase.execute(input)
                val elapsed = System.currentTimeMillis() - startTime
                renderVerificationResult(dialogView, result, elapsed)
            }
        }

        btnSelectPdf?.setOnClickListener {
            selectPdfLauncher.launch("application/pdf")
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setOnDismissListener { activeVerifyDialogView = null }
            .create()

        btnClose?.setOnClickListener { dialog.dismiss() }
        dialog.show()

        if (presetCert != null) {
            btnVerifyQr?.performClick()
        }
    }

    private fun renderVerificationResult(rootView: View, result: VerificationResult, elapsedMs: Long) {
        val cardResult = rootView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_verification_result)
        val tvStatus = rootView.findViewById<TextView>(R.id.tv_verify_status)
        val tvDetails = rootView.findViewById<TextView>(R.id.tv_verify_details)
        val tvSpeedNote = rootView.findViewById<TextView>(R.id.tv_verify_speed_note)

        if (cardResult == null || tvStatus == null || tvDetails == null) return

        cardResult.visibility = View.VISIBLE
        val secondsStr = String.format(Locale.getDefault(), "%.2f", elapsedMs / 1000.0)
        tvSpeedNote?.text = "⚡ Verificado en $secondsStr s (100% offline sin datos sensibles expuestos)"

        when (result) {
            is VerificationResult.Valid -> {
                tvStatus.text = "✅ CERTIFICADO AUTÉNTICO"
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.meeting_point_text))
                cardResult.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.meeting_point_bg))
                cardResult.strokeColor = ContextCompat.getColor(requireContext(), R.color.meeting_point_border)

                val p = result.payload
                tvDetails.text = "• Otorgado a: ${p.userName}\n• Nivel: ${p.level.name}\n• Puntaje: ${p.score}/${p.maxScore}\n• Certificado N.º: ${p.certificateId}"
            }
            is VerificationResult.Tampered -> {
                tvStatus.text = "❌ CERTIFICADO ALTERADO"
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
                cardResult.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_container))
                cardResult.strokeColor = ContextCompat.getColor(requireContext(), R.color.color_error)

                tvDetails.text = "Motivo: ${result.reason}\n\nAtención: La firma digital o contenido PDF no coincide con los registros auténticos."
            }
            VerificationResult.NotFound -> {
                tvStatus.text = "⚠️ NO REGISTRADO EN CRISISSIM"
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.incendios_badge_text))
                cardResult.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.incendios_badge_bg))
                cardResult.strokeColor = ContextCompat.getColor(requireContext(), R.color.incendios_badge_text)

                tvDetails.text = "Este certificado no corresponde a un perfil o certificado registrado en este dispositivo."
            }
            VerificationResult.InvalidFormat -> {
                tvStatus.text = "⚠️ FORMATO NO RECONOCIDO"
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.incendios_badge_text))
                cardResult.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.incendios_badge_bg))
                cardResult.strokeColor = ContextCompat.getColor(requireContext(), R.color.incendios_badge_text)

                tvDetails.text = "El código o archivo ingresado no tiene un formato de certificado CrisisSim."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── Adapter Interno ───

    private class CertificateAdapter(
        private val items: List<CertificateEntity>,
        private val lifecycleScope: kotlinx.coroutines.CoroutineScope,
        private val fragment: CertificatesFragment,
        private val generatePdfUseCase: GenerateCertificatePdfUseCase,
        private val snackbarView: View,
        private val onVerifyItem: (CertificateEntity) -> Unit
    ) : RecyclerView.Adapter<CertificateAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        class ViewHolder(val binding: ItemCertificateBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemCertificateBinding.inflate(inflater, parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvCertTitle.text = "Certificado Nivel: ${item.level}"
            holder.binding.tvCertUser.text = "Otorgado a: ${item.userName}"
            holder.binding.tvCertDetails.text = "Puntaje: ${item.score}/${item.maxScore} · Firma QR: ${item.signature.take(16)}"
            holder.binding.tvCertDate.text = "Emitido el: ${dateFormat.format(Date(item.issuedAt))}"

            // Al tocar la tarjeta se abre la verificación
            holder.binding.root.setOnClickListener {
                onVerifyItem(item)
            }

            // Botón Vista Previa
            holder.binding.btnCertPreview.setOnClickListener {
                showPdfPreview(item)
            }

            // Botón Compartir PDF
            holder.binding.btnCertShare.setOnClickListener {
                sharePdf(item)
            }
        }

        override fun getItemCount(): Int = items.size

        private fun showPdfPreview(item: CertificateEntity) {
            lifecycleScope.launch {
                val pdfUri = generatePdfUseCase.execute(fragment.requireContext(), item.id)
                if (pdfUri != null) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(pdfUri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        fragment.startActivity(intent)
                    } catch (e: Exception) {
                        Snackbar.make(snackbarView, "No hay visor de PDF instalado", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    Snackbar.make(snackbarView, "Error generando PDF", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        private fun sharePdf(item: CertificateEntity) {
            lifecycleScope.launch {
                val pdfUri = generatePdfUseCase.execute(fragment.requireContext(), item.id)
                if (pdfUri != null) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Certificado CrisisSim - ${item.level}")
                        putExtra(Intent.EXTRA_TEXT, "Certificado de ${item.userName} - Nivel ${item.level} (${item.score}/${item.maxScore})")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    val chooser = Intent.createChooser(shareIntent, fragment.getString(com.example.crissim.R.string.certs_share_chooser))
                    try {
                        fragment.startActivity(chooser)
                    } catch (e: Exception) {
                        Snackbar.make(snackbarView, "Error compartiendo PDF", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    Snackbar.make(snackbarView, "Error generando PDF", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}
