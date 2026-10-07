package com.example.cardtally

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.cardtally.update.AppUpdateViewModel
import com.example.cardtally.update.GitHubRelease
import com.google.android.material.button.MaterialButton

class AboutFragment : Fragment(R.layout.fragment_about) {
    private lateinit var updates: AppUpdateViewModel
    private var confirmation: AlertDialog? = null
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT < 26 || requireContext().packageManager.canRequestPackageInstalls()) installReady()
        else toast(R.string.about_permission_denied)
    }
    private val installerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updates = ViewModelProvider(this)[AppUpdateViewModel::class.java]
        view.findViewById<TextView>(R.id.text_title).setText(R.string.about_title)
        view.findViewById<View>(R.id.btn_back).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<TextView>(R.id.about_version).text = getString(R.string.about_version_value,
            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, if (BuildConfig.DEBUG) "dev" else "release")
        view.findViewById<View>(R.id.about_releases).setOnClickListener {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GitHubRelease.RELEASES_URL))) }
            catch (_: Exception) { toast(R.string.about_open_failed) }
        }
        val action = view.findViewById<MaterialButton>(R.id.about_check)
        val cancel = view.findViewById<View>(R.id.about_cancel)
        val progress = view.findViewById<ProgressBar>(R.id.about_progress)
        val status = view.findViewById<TextView>(R.id.about_status)
        cancel.setOnClickListener { updates.cancelDownload() }
        updates.state.observe(viewLifecycleOwner) { state ->
            progress.visibility = if (state is AppUpdateViewModel.State.Downloading) View.VISIBLE else View.GONE
            cancel.visibility = progress.visibility
            action.isEnabled = state !is AppUpdateViewModel.State.Checking && state !is AppUpdateViewModel.State.Downloading
            action.setText(when (state) {
                is AppUpdateViewModel.State.Available -> R.string.about_download
                is AppUpdateViewModel.State.Ready -> R.string.about_install
                else -> R.string.about_check
            })
            status.text = when (state) {
                AppUpdateViewModel.State.Idle -> getString(R.string.about_check_hint)
                AppUpdateViewModel.State.Checking -> getString(R.string.about_checking)
                AppUpdateViewModel.State.Current -> getString(R.string.about_current)
                AppUpdateViewModel.State.NoPackage -> getString(R.string.about_no_package)
                is AppUpdateViewModel.State.Available -> getString(R.string.about_available, state.release.version.toString(), state.release.notes)
                is AppUpdateViewModel.State.Downloading -> { progress.progress = state.percent; getString(R.string.about_downloading, state.percent) }
                is AppUpdateViewModel.State.Ready -> getString(R.string.about_ready)
                is AppUpdateViewModel.State.Failed -> getString(if (state.verification) R.string.about_verify_failed else R.string.about_network_failed)
            }
            action.setOnClickListener {
                when (state) {
                    is AppUpdateViewModel.State.Available -> {
                        confirmation?.dismiss()
                        confirmation = AlertDialog.Builder(requireContext()).setTitle(R.string.about_download)
                            .setMessage(getString(R.string.about_confirm_download, state.release.version.toString(), state.release.size / 1024 / 1024.0))
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.about_download) { _, _ -> updates.download(requireContext(), state.release) }.show()
                    }
                    is AppUpdateViewModel.State.Ready -> {
                        if (Build.VERSION.SDK_INT >= 26 && !requireContext().packageManager.canRequestPackageInstalls()) {
                            confirmation = AlertDialog.Builder(requireContext()).setMessage(R.string.about_permission_hint)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(R.string.about_open_settings) { _, _ ->
                                    try { permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${requireContext().packageName}"))) }
                                    catch (_: Exception) { toast(R.string.about_open_failed) }
                                }.show()
                        } else installReady()
                    }
                    else -> updates.check()
                }
            }
        }
    }

    private fun installReady() {
        val ready = updates.state.value as? AppUpdateViewModel.State.Ready ?: return
        if (!ready.file.isFile) { toast(R.string.about_file_missing); updates.check(); return }
        try {
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", ready.file)
            installerLauncher.launch(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply { clipData = ClipData.newRawUri("update", uri) })
        } catch (_: Exception) { toast(R.string.about_open_failed) }
    }

    private fun toast(message: Int) { context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() } }

    override fun onDestroyView() {
        confirmation?.dismiss()
        confirmation = null
        super.onDestroyView()
    }
}
