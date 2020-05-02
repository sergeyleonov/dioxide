package com.sleonov.dioxide.hardware

import com.sleonov.dioxide.types.Empty
import com.sleonov.dioxide.types.Error
import com.sleonov.dioxide.types.Result

interface UsbHelper {

    fun enumerate(vid: Int, pid: Int, nInterface: Int): Result<Error, Empty>

    fun open(): Result<Error, Empty>

    fun close()

    fun isConnected(): Result<Error, Empty>

    fun controlTransfer(
        requestType: Int, request: Int, value: Int,
        index: Int, buffer: ByteArray, length: Int, timeout: Int
    ): Int?

    fun readReport(size: Int): ByteArray?
}
