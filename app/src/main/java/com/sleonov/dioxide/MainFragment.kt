package com.sleonov.dioxide

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.sleonov.dioxide.types.Error
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {

    val TAG = "MainFragment"

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        connectButton.setOnClickListener {
            viewModel.connectButtonPressed()
        }

        viewModel.dioxideText.observe(this, Observer {
            dioxideText.text = it
        })

        viewModel.temperatureText.observe(this, Observer {
            temperatureText.text = it
        })

        viewModel.usbOperationError.observe(this, Observer {
            when (it) {
                is Error.UsbConnectionError -> showMessage(getString(R.string.error_connection))
                is Error.ClaimInterfaceError -> showMessage(getString(R.string.error_claim_interface))
                is Error.InitDeviceError -> showMessage(getString(R.string.error_init_device))
                is Error.NoDeviceFoundError -> showMessage(getString(R.string.error_no_device_found))
                is Error.ReadReportError -> showMessage(getString(R.string.error_read_report))
                is Error.CrcError -> showMessage(getString(R.string.error_crc))
                is Error.FormatError -> showMessage(getString(R.string.error_format))
                /* handle other errors */
            }
            connectButton.text = getString(R.string.connect_hint)
        })

        viewModel.connectionLiveData.observe(this, Observer {
            showMessage(getString(R.string.connection_state_success))
            connectButton.text = getString(R.string.disconnect_hint)
        })

        viewModel.disconnectionLiveData.observe(this, Observer {
            showMessage(getString(R.string.disconnection_success))
            connectButton.text = getString(R.string.connect_hint)
        })

        // Toggle immersive mode
        toggleHideyBar()
    }

    private fun showMessage(message: String) {
        (activity as MainActivity).showMessage(message)
    }

    /**
     * Detects and toggles immersive mode (also known as "hidey bar" mode).
     */
    fun toggleHideyBar() {

        // BEGIN_INCLUDE (get_current_ui_flags)
        // The UI options currently enabled are represented by a bitfield.
        // getSystemUiVisibility() gives us that bitfield.
        val uiOptions = activity!!.window.decorView.systemUiVisibility
        var newUiOptions = uiOptions
        // END_INCLUDE (get_current_ui_flags)
        // BEGIN_INCLUDE (toggle_ui_flags)
        val isImmersiveModeEnabled =
            uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY == uiOptions
        if (isImmersiveModeEnabled) {
            Log.i(TAG, "Turning immersive mode mode off")
        } else {
            Log.i(TAG, "Turning immersive mode mode on")
        }

        // Navigation bar hiding:  Backwards compatible to ICS.
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions = newUiOptions xor View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions = newUiOptions xor View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        // Immersive mode: Backward compatible to KitKat.
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
        // Sticky immersive mode differs in that it makes the navigation and status bars
        // semi-transparent, and the UI flag does not get cleared when the user interacts with
        // the screen.
        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions = newUiOptions xor View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        activity!!.window.decorView.systemUiVisibility = newUiOptions
        //END_INCLUDE (set_ui_flags)
    }
}
