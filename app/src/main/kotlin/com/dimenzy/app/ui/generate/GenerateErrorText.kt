package com.dimenzy.app.ui.generate

import com.dimenzy.app.data.GenerateError

/** The one place generation errors become words (English only, spec 0002). */
fun GenerateError.message(): String = when (this) {
  GenerateError.MissingPhoto -> "The photo is gone. Choose another."
  GenerateError.BadImage -> "That photo couldn't be used. Try another."
  GenerateError.BadKey -> "API key problem. Check local.properties."
  GenerateError.OutOfCredits -> "Out of AI credits. Try the sample model."
  GenerateError.Refused -> "The service refused this image."
  GenerateError.RateLimited -> "Too many requests. Wait a minute."
  GenerateError.Server -> "The AI service had a problem. Retry."
  GenerateError.Timeout -> "Generation timed out. Retry."
  GenerateError.NoNetwork -> "No internet connection."
  GenerateError.Interrupted -> "Generation was interrupted. Retry to send it again."
  is GenerateError.Unexpected -> "Something went wrong. Retry."
}
