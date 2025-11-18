
package app.crossword.yourealwaysbe.forkyz.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContract

/**
 * Voice recognition contract
 *
 * Takes speech model (e.g. RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).
 * Returns array of strings heard.
 */
class SpeechContract : ActivityResultContract<String, List<String>>() {
    override fun createIntent(
        context : Context,
        input : String,
    ) : Intent {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            input,
        )
        return intent
    }

    override fun parseResult(
        resultCode : Int,
        intent : Intent?,
    ) : List<String> {
        return if (resultCode == Activity.RESULT_OK) {
            return intent?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            ) ?: listOf()
        } else {
            return listOf()
        }
    }
}

