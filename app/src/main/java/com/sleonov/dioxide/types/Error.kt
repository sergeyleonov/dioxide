package com.sleonov.dioxide.types

sealed class Error {

    object UsbConnectionError : Error()
    object ClaimInterfaceError : Error()
    object InitDeviceError : Error()
    object NoDeviceFoundError : Error()
    object ReadReportError : Error()
    object CrcError : Error()
    object FormatError : Error()
    object NonValueError : Error()
}
