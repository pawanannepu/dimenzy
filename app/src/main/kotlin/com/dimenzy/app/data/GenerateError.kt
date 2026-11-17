package com.dimenzy.app.data

/** Every way a generation can fail (spec 0002 error table). The UI maps each case to a message with Retry. */
sealed interface GenerateError {
  /** `upload.jpg` doesn't exist. */
  data object MissingPhoto : GenerateError

  /** `400`, `422`. */
  data object BadImage : GenerateError

  /** `401`. */
  data object BadKey : GenerateError

  /** `402`. */
  data object OutOfCredits : GenerateError

  /** `403`. */
  data object Refused : GenerateError

  /** `429`. */
  data object RateLimited : GenerateError

  /** `5xx`. */
  data object Server : GenerateError

  /** `SocketTimeoutException` or `InterruptedIOException`. */
  data object Timeout : GenerateError

  /** Any other `IOException`. */
  data object NoNetwork : GenerateError

  /** Android restored the generate screen after the call started, with no result. Never resent without a tap. */
  data object Interrupted : GenerateError

  /** `413`, any other status, or any other exception. [code] is the HTTP status when there was one. */
  data class Unexpected(val code: Int?) : GenerateError
}

/** The only result the UI sees from [ModelRepository.generate]. */
sealed interface GenerateOutcome {
  data class Success(val modelId: String) : GenerateOutcome

  data class Failure(val error: GenerateError) : GenerateOutcome
}
