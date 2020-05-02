package com.sleonov.dioxide

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.sleonov.dioxide.device.DioxideDeviceImpl
import com.sleonov.dioxide.hardware.UsbHelperImpl
import com.sleonov.dioxide.types.Empty
import com.sleonov.dioxide.types.Error
import com.sleonov.dioxide.types.Value
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val customDevice = DioxideDeviceImpl(UsbHelperImpl(application.applicationContext))
    private val disposable = CompositeDisposable()

    // Save values to update them only on change to save some power
    private var temperature: Double = 0.0
    private var carbonDioxidePpm: Int = 0

    val textView = MutableLiveData<String>()
    val dioxideText = MutableLiveData<String>()
    val temperatureText = MutableLiveData<String>()
    val usbOperationError = MutableLiveData<Error>()
    val usbOperationSuccess = MutableLiveData<Empty>()

    fun connectButtonPressed() {
        if (customDevice.isConnected().isSuccess)
            customDevice.disconnect()
        else {
            customDevice.connect().handle(::handleError, ::handleConnect)
            customDevice.initialize().handle(::handleError, ::handleInitialize)
        }
    }

    private fun handleError(error: Error) {
        usbOperationError.postValue(error)
    }

    private fun handleConnect(success: Empty) {
        usbOperationSuccess.postValue(success)
    }

    private fun handleInitialize(success: Empty) {
        setDioxideObserver()
        usbOperationSuccess.postValue(success)
    }

    private fun handleValue(value: Value) {
        if (value.temperature != 0.0 && temperature != value.temperature) {
            temperature = value.temperature
            temperatureText.postValue("%.1f °C".format(value.temperature))
        }
        if (value.carbonDioxidePpm != 0 && carbonDioxidePpm != value.carbonDioxidePpm) {
            carbonDioxidePpm = value.carbonDioxidePpm
            dioxideText.postValue(value.carbonDioxidePpm.toString() + " ppm")
        }
    }

    private fun setDioxideObserver() {
        disposable.add(
            customDevice.receive()
                .delay(500, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .repeat()
                .subscribe({
                    it.handle(::handleError, ::handleValue)
                }, {
                    usbOperationError.postValue(Error.ReadReportError)
                })
        )
    }

    override fun onCleared() {
        super.onCleared()
        customDevice.disconnect()
    }
}
