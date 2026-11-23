package com.example.crissim.presentation.ui.certificates

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.crissim.CrisisSimApp
import com.example.crissim.data.local.entity.CertificateEntity
import com.example.crissim.databinding.FragmentCertificatesBinding
import com.example.crissim.databinding.ItemCertificateBinding
import com.example.crissim.domain.usecase.GenerateCertificatePdfUseCase
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de Certificados Digitales (HU-18, HU-19).
 * Lista los certificados obtenidos con firma hash/QR.
 * Permite vista previa y compartir PDF.
 */
class CertificatesFragment : Fragment() {

    private var _binding: FragmentCertificatesBinding? = null
    private val binding get() = _binding!!

    // UseCase para generar PDF
    private val generatePdfUseCase: GenerateCertificatePdfUseCase by lazy {
        val container = CrisisSimApp.instance.container
        GenerateCertificatePdfUseCase(container.certificateRepository)
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
        loadCertificates()
    }

    private fun loadCertificates() {
        lifecycleScope.launch {
            val userRepo = CrisisSimApp.instance.container.userRepository
            val activeUser = userRepo.getActiveUser()
            if (activeUser == null) {
                binding.tvEmpty.isVisible = true
                binding.rvCertificates.isVisible = false
                return@launch
            }

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
                    snackbarView = binding.root
                )
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
        private val snackbarView: View
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
                    // Abrir PDF con visor del sistema
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
