package com.sleonov.dioxide.device

import android.hardware.usb.UsbConstants
import android.util.Log
import com.sleonov.dioxide.hardware.UsbHelper
import com.sleonov.dioxide.types.Empty
import com.sleonov.dioxide.types.Error
import com.sleonov.dioxide.types.Result
import com.sleonov.dioxide.types.Value
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class DioxideDeviceImpl(
    private val usbHelper: UsbHelper
) : DioxideDevice {

    private val MAGIC_WORD = "Htemp99e".toByteArray()
    private val magicWordWrapped = ByteArray(MAGIC_WORD.size)

    // Magic table encodes the responses. It can be empty.
    private val MAGIC_TABLE = ByteArray(8)

    companion object {
        const val VENDOR_ID = 0x04d9
        const val PRODUCT_ID = 0xa052
        const val CUSTOM_HID_INTERFACE_ID = 0x00
        const val REPORT_SIZE = 8
    }

    init {
        for (i in MAGIC_WORD.indices) {
            magicWordWrapped[i] =
                (MAGIC_WORD[i].toInt() shl 4 or (MAGIC_WORD[i].toInt() shr 4)).toByte()
        }
    }

    override fun connect(): Result<Error, Empty> =
        usbHelper.enumerate(
            VENDOR_ID,
            PRODUCT_ID,
            CUSTOM_HID_INTERFACE_ID
        )

    override fun initialize(): Result<Error, Empty> {
        // Send empty magic table. Magic table is used to encode the meter responses. Empty magic table leaves responses unencoded.
        val bytesTransmitted = usbHelper.controlTransfer(
            UsbConstants.USB_DIR_OUT + UsbConstants.USB_TYPE_CLASS + 1,
            0x09,
            0x0300,
            0,
            MAGIC_TABLE,
            MAGIC_TABLE.size,
            2000
        )
        if ((bytesTransmitted == null) or (bytesTransmitted != MAGIC_TABLE.size)) {
            Log.i("Helper", "bytesTransmitted = $bytesTransmitted")
            return Result.Failure(Error.InitDeviceError)
        }
        return Result.Success(Empty())
    }

    override fun disconnect() {
        usbHelper.close()
    }

    override fun isConnected(): Result<Error, Empty> =
        usbHelper.isConnected()

    override fun receive(): Observable<Result<Error, Value>> {
        return Observable.fromCallable { readItAll() }
            .subscribeOn(Schedulers.io())
    }

    private fun readItAll(): Result<Error, Value> {
        val result = usbHelper.readReport(REPORT_SIZE)
        return if (result == null) {
            Result.Failure(Error.UsbConnectionError)
        } else {
            decodeValue(result)
        }
    }

    private fun decodeValue(report: ByteArray): Result<Error, Value> {
        val decodedBytes = decodeBuffer(report)
        if (decodedBytes[4].toInt() != 0x0d) {
            return Result.Failure(Error.FormatError)
        }
        if ((decodedBytes[0] + decodedBytes[1] + decodedBytes[2]).toByte() != decodedBytes[3]) {
            return Result.Failure(Error.CrcError)
        }
        val code = decodedBytes[0].toInt()
        // Convert signed bytes into unsigned integers
        val r1: Int = (decodedBytes[1] + 256) % 256
        val r2: Int = (decodedBytes[2] + 256) % 256
        val value = (r1 shl 8) + r2
        if (code == 0x42) {
            return Result.Success(Value(0, value * 0.0625 - 273.15))
        } else if (code == 0x50) {
            return Result.Success(Value(value, 0.0))
        }
        return Result.Success(Value(0, 0.0))
    }

    private fun swapBytes(buf: ByteArray, idx1: Int, idx2: Int) {
        val b = buf[idx1]
        buf[idx1] = buf[idx2]
        buf[idx2] = b
    }

    private fun shift(b1: Byte, b2: Byte): Byte {
        var v1 = (b1.toInt() shl 5).toByte()
        v1 = (v1.toInt() and 0xe0).toByte()
        var v2 = (b2.toInt() shr 3).toByte()
        v2 = (v2.toInt() and 0x1f).toByte()
        return (v1.toInt() or v2.toInt()).toByte()
    }

    private fun decodeBuffer(buffer: ByteArray): ByteArray {
        val result = ByteArray(buffer.size)

        swapBytes(buffer, 0, 2)
        swapBytes(buffer, 1, 4)
        swapBytes(buffer, 3, 7)
        swapBytes(buffer, 5, 6)

        for (i in buffer.indices) {
            buffer[i] = (buffer[i].toInt() xor MAGIC_TABLE[i].toInt()).toByte()
        }

        result[7] = shift(buffer[6], buffer[7])
        result[6] = shift(buffer[5], buffer[6])
        result[5] = shift(buffer[4], buffer[5])
        result[4] = shift(buffer[3], buffer[4])
        result[3] = shift(buffer[2], buffer[3])
        result[2] = shift(buffer[1], buffer[2])
        result[1] = shift(buffer[0], buffer[1])
        result[0] = shift(buffer[7], buffer[0])

        for (i in result.indices) {
            result[i] = (result[i] - magicWordWrapped[i]).toByte()
        }
        return result
    }
}
