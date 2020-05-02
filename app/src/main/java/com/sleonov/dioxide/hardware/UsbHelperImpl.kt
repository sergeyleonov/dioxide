package com.sleonov.dioxide.hardware

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.*
import android.util.Log
import com.sleonov.dioxide.types.Empty
import com.sleonov.dioxide.types.Error
import com.sleonov.dioxide.types.Result
import java.nio.ByteBuffer

class UsbHelperImpl(context: Context) : UsbHelper {

    companion object {
        private const val COMPLETE = true
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private lateinit var usbInterface: UsbInterface
    private lateinit var inRequest: UsbRequest
    private lateinit var usbDevice: UsbDevice

    private var mPermissionIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent("com.android.example.USB_PERMISSION"),
        0
    )

    private var usbInEndpoint: UsbEndpoint? = null
    private var usbOutEndpoint: UsbEndpoint? = null

    private var usbConnection: UsbDeviceConnection? = null

    override fun enumerate(vid: Int, pid: Int, nInterface: Int): Result<Error, Empty> {
        usbDevice = findDevice(vid, pid) ?: return Result.Failure(Error.NoDeviceFoundError)

        // todo: do it asynchronously
        usbManager.requestPermission(usbDevice, mPermissionIntent)

        usbInterface = usbDevice.getInterface(nInterface)

        for (num in 0 until usbInterface.endpointCount) {
            if (usbInterface.getEndpoint(num).direction == UsbConstants.USB_DIR_IN) {
                usbInEndpoint = usbInterface.getEndpoint(num)
            } else {
                usbOutEndpoint = usbInterface.getEndpoint(num)
            }
        }

        return open()
    }

    override fun open(): Result<Error, Empty> {
        usbConnection = usbManager.openDevice(usbDevice)
        val result = usbConnection?.claimInterface(usbInterface, true)

        if ((result == null) or (result != COMPLETE)) {
            return Result.Failure(Error.ClaimInterfaceError)
        }

        inRequest = UsbRequest()
        inRequest.initialize(usbConnection, usbInEndpoint)

        return Result.Success(Empty())
    }

    override fun close() {
        usbConnection?.let {
            inRequest.close()
            it.releaseInterface(usbInterface)
            it.close()
        }
    }

    override fun readReport(size: Int): ByteArray? {
        // todo: nio memory leak?
        val buffer = ByteBuffer.allocate(size)

        usbConnection?.let {
            return if (inRequest.queue(buffer, size)) {
                usbConnection?.requestWait()

                buffer.rewind()
                val report = ByteArray(size)
                buffer.get(report, 0, report.size)
                buffer.clear()
                report
            } else {
                null
            }
        } ?: return null
    }

    override fun isConnected(): Result<Error, Empty> {
        usbConnection ?: return Result.Failure(Error.UsbConnectionError)
        return Result.Success(Empty())
    }

    override fun controlTransfer(
        requestType: Int, request: Int, value: Int,
        index: Int, buffer: ByteArray, length: Int, timeout: Int
    ): Int? {
        return usbConnection?.controlTransfer(
            requestType,
            request,
            value,
            index,
            buffer,
            length,
            timeout
        )
    }

    private fun findDevice(vid: Int, pid: Int): UsbDevice? {
        Log.i("Helper", "deviceList: " + usbManager.deviceList.toString())
        usbManager.deviceList.values.forEach { device ->
            if ((device.vendorId == vid) and (device.productId == pid)) {
                return device
            }
        }
        return null
    }
}
