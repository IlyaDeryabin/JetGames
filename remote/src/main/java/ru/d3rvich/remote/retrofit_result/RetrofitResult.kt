package ru.d3rvich.remote.retrofit_result

/**
 * Created by Ilya Deryabin at 09.02.2024
 */
sealed class RetrofitResult<out T> {

    sealed class Success<T> : RetrofitResult<T>() {

        abstract val value: T

        override fun toString() = "Success($value)"

        class Value<T>(override val value: T) : Success<T>()

        data class HttpResponse<T>(
            override val value: T,
            override val statusCode: Int,
            override val statusMessage: String? = null,
            override val url: String? = null
        ) : Success<T>(), ru.d3rvich.remote.retrofit_result.HttpResponse

        object Empty : Success<Nothing>() {

            override val value: Nothing get() = error("No value")

            override fun toString() = "Success"
        }
    }

    sealed class Failure<E : Throwable>(open val error: E? = null) : RetrofitResult<Nothing>() {

        override fun toString() = "Failure($error)"

        class Error(override val error: Throwable) : Failure<Throwable>(error)

        class HttpError(override val error: HttpException) : Failure<HttpException>(),
            HttpResponse {

            override val statusCode: Int get() = error.statusCode

            override val statusMessage: String? get() = error.statusMessage

            override val url: String? get() = error.url
        }
    }
}

typealias EmptyResult = RetrofitResult<Nothing>