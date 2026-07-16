
package app.crossword.yourealwaysbe.forkyz

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.toForkyzUri

/**
 * Import settings json and exit
 **/
class ForkyzSettingsImporter : AppCompatActivity() {
    private lateinit var uri : Uri

    private val viewModel : ForkyzSettingsImporterViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass : Class<T>) : T {
                @Suppress("UNCHECKED_CAST")
                return ForkyzSettingsImporterViewModel(uri) as T
            }
        }
    }

    override protected fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = getIntentUri(getIntent())
        if (uri == null) {
            finish()
            return
        }
        this.uri = uri

        setContent {
            val finishMessage by viewModel.finishMessage.collectAsState()

            LaunchedEffect(finishMessage) {
                finishMessage?.let {
                    Toast
                        .makeText(applicationContext, it, Toast.LENGTH_LONG)
                        .show()
                    finish()
                }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.preferences_importing))
                    }
                }
            }
        }
    }

    private fun getIntentUri(intent : Intent) : Uri? {
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.toForkyzUri()
            Intent.ACTION_SEND ->
                IntentCompat.getParcelableExtra(
                    intent,
                    Intent.EXTRA_STREAM,
                    android.net.Uri::class.java,
                )?.toForkyzUri()
            else -> null
        }
    }
}

