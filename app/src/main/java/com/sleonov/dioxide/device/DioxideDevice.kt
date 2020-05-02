package com.sleonov.dioxide.device

import com.sleonov.dioxide.types.Empty
import com.sleonov.dioxide.types.Error
import com.sleonov.dioxide.types.Result
import com.sleonov.dioxide.types.Value
import io.reactivex.Observable

interface DioxideDevice {

    fun connect(): Result<Error, Empty>

    fun initialize(): Result<Error, Empty>

    fun disconnect()

    fun isConnected(): Result<Error, Empty>

    fun receive(): Observable<Result<Error, Value>>
}
