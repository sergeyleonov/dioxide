package com.sleonov.dioxide.types

sealed class Result<out E, out D> {

    data class Failure<out E>(val error: E) : Result<E, Nothing>()
    data class Success<out D>(val data: D) : Result<Nothing, D>()

    val isSuccess get() = this is Success<D>

    fun handle(handleError: (E) -> Any, handleSuccess: (D) -> Any): Any =
        when (this) {
            is Failure -> handleError(error)
            is Success -> handleSuccess(data)
        }

}
