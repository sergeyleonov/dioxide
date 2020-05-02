package com.sleonov.dioxide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.sleonov.dioxide.types.Error
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {

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

        viewModel.textView.observe(this, Observer {
            textView.text = it
        })

        viewModel.usbOperationError.observe(this, Observer {
            when (it) {
                is Error.NoDeviceFoundError -> showMessage(getString(R.string.error_no_device_found))
                is Error.UsbConnectionError -> showMessage(getString(R.string.error_connection))
                is Error.ClaimInterfaceError -> showMessage(getString(R.string.error_claim_interface))
                is Error.InitDeviceError -> showMessage(getString(R.string.error_init_device))
                is Error.ReadReportError -> showMessage(getString(R.string.error_read_report))
                /* handle other errors */
            }
            connectButton.text = getString(R.string.connect_hint)
        })

        viewModel.usbOperationSuccess.observe(this, Observer {
            showMessage(getString(R.string.connection_state_success))
            connectButton.text = getString(R.string.disconnect_hint)
        })

    }

    private fun showMessage(message: String) {
        (activity as MainActivity).showMessage(message)
    }
}
