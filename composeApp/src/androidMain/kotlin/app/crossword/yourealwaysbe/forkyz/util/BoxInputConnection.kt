
package app.crossword.yourealwaysbe.forkyz.util

import java.util.Locale

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Selection
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import androidx.core.view.inputmethod.EditorInfoCompat

import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

class BoxInputConnection(
    private val targetView : View,
    private val utils : AndroidVersionUtils?,
    initialResponse : String?,
    listener : BoxInputListener?,
    forceCaps : Boolean,
)  : BaseInputConnection(targetView, true) {
    private val boardEditable : BoardEditable
    private val listener : BoxInputListener?
    private val isForceCaps : Boolean
    private var updateExtractedTextToken = -1
    private var batchEditNestingDepth = 0
    private var pendingResponse : String? = null

    interface BoxInputListener {
        fun onNewResponse(response : String?) {}
        fun onDeleteResponse() {}
    }

    /**
     * Create BoxInputConnection
     *
     * @param forceCaps whether to convert all input into uppercase (input is
     * always requested in uppercase, but not always enforced by input method,
     * this setting converts it)
     */
    init {
        this.boardEditable = BoardEditable(initialResponse)
        this.listener = listener
        this.isForceCaps = forceCaps
    }

    fun setOutAttrs(outAttrs : EditorInfo) {
        outAttrs.fieldId = targetView.getId()
        outAttrs.inputType = INPUT_TYPE
        outAttrs.imeOptions = EditorInfo.IME_NULL
        outAttrs.initialSelStart = boardEditable.getSpanStart(
            Selection.SELECTION_START,
        )
        outAttrs.initialSelEnd = boardEditable.getSpanStart(
            Selection.SELECTION_END,
        )
        outAttrs.packageName = targetView.getContext().getPackageName()
        outAttrs.initialCapsMode = TextUtils.CAP_MODE_CHARACTERS
        EditorInfoCompat.setInitialSurroundingText(
            outAttrs,
            boardEditable.toString(),
        )
    }

    override fun getEditable() : Editable {
        return boardEditable
    }

    override fun getExtractedText(
        request : ExtractedTextRequest?, flags : Int
    ) : ExtractedText {
        // call with null internally for convenience
        if (request != null) {
            if ((flags and GET_EXTRACTED_TEXT_MONITOR) != 0)
                updateExtractedTextToken = request.token
        }

        val extracted = ExtractedText()
        extracted.flags = 0
        extracted.partialStartOffset = -1
        extracted.selectionStart = 0
        extracted.startOffset = 0
        val response = boardEditable.toString()
        extracted.text = response
        extracted.selectionEnd = response.length

        return extracted
    }

    fun setResponse(response : String?) {
        boardEditable.setResponse(response)
    }

    fun refreshInput() {
        val imm = this.inputMethodManager
        // Don't use invalidateInput -- it is more efficient but leads
        // to what looks like a race condition with showSoftInput. It
        // starts a new session ID but showSoftInput tries to connect to
        // the wrong one and keyboard entry breaks. This is in the logs :
        // "RemoteInputConnectionImpl : Session id mismatch
        // header.sessionId : 0 currentSessionId : 1"
        imm.restartInput(targetView)
        //utils.invalidateInput(imm, targetView);
    }

    override fun beginBatchEdit() : Boolean {
        batchEditNestingDepth += 1
        return true
    }

    override fun endBatchEdit() : Boolean {
        if (batchEditNestingDepth > 0)
            batchEditNestingDepth -= 1

        if (this.isBatchEdit) {
            return true
        } else {
            if (pendingResponse != null) {
                // need to reset pending as some connections call
                // begin/endBatchEdit during an update, so
                // pendingResponse needs to be cleared before updating
                // else infinite loop
                val savedPendingResponse = pendingResponse
                pendingResponse = null
                updateListenerWithResponse(savedPendingResponse!!)
            }
            return false
        }
    }

    private val isBatchEdit : Boolean
        get() = batchEditNestingDepth > 0

    /**
     * Register an update to response with listener
     *
     * If in a batch edit, the response will be delayed until batch
     * done. Subsequent updates will overwrite previous pending.
     */
    private fun updateListenerWithResponse(response : String) {
        var response = response
        if (this.isBatchEdit) {
            pendingResponse = response
        } else {
            if (listener != null) {
                // if removing characters, delete
                if (response.isEmpty()) {
                    listener.onDeleteResponse()
                } else {
                    if (this.isForceCaps)
                        response = response.uppercase(Locale.getDefault())
                    // give whole response so emojis work
                    listener.onNewResponse(response)
                }
            }
        }
    }

    private val inputMethodManager : InputMethodManager
        get() = targetView
            .getContext()
            .getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

    private inner class BoardEditable(initialResponse : String?)  : Editable {
        var spannable : SpannableStringBuilder

        init {
            spannable = SpannableStringBuilder(initialResponse)
            Selection.selectAll(spannable)
        }

        override fun append(text : Char) : Editable {
            return append(text.toString())
        }

        override fun append(
            text : CharSequence?,
            start : Int,
            end : Int,
        ) : Editable {
            return replace(length, length, text, start, end)
        }

        override fun append(text : CharSequence?) : Editable {
            return replace(length, length, text, 0, text?.length ?: 0)
        }

        override fun clear() {
            replace(0, length, "", 0, 0)
        }

        override fun clearSpans() {
            spannable.clearSpans()
        }

        override fun delete(st : Int, en : Int) : Editable {
            return replace(st, en, "", 0, 0)
        }

        override fun getFilters() : Array<InputFilter?>? {
            return spannable.getFilters()
        }

        override fun insert(where : Int, text : CharSequence) : Editable {
            return replace(where, where, text, 0, text.length)
        }

        override fun insert(
            where : Int,
            text : CharSequence?,
            start : Int,
            end : Int,
        ) : Editable {
            return replace(where, where, text, start, end)
        }

        override fun replace(
            st : Int,
            en : Int,
            source : CharSequence?,
            start : Int,
            end : Int,
        ) : Editable {
            spannable.replace(st, en, source, start, end)
            updateListenerWithResponse(spannable.toString())
            return this
        }

        override fun replace(
            st : Int,
            en : Int,
            text : CharSequence,
        ) : Editable {
            return replace(st, en, text, 0, text.length)
        }

        override fun setFilters(filters : Array<InputFilter?>?) {
            spannable.setFilters(filters)
        }

        override fun get(index : Int) : Char {
            return spannable.get(index)
        }

        override val length : Int
            get() { return spannable.length }

        override fun subSequence(
            startIndex : Int,
            endIndex : Int,
        ) : CharSequence {
            return spannable.subSequence(startIndex, endIndex)
        }

        override fun toString() : String {
            return spannable.toString()
        }

        override fun getChars(
            start : Int,
            end : Int,
            dest : CharArray,
            destoff : Int,
        ) {
            spannable.toString().toCharArray(dest, destoff, start, end)
        }

        override fun removeSpan(what : Any?) {
            spannable.removeSpan(what)
        }

        override fun setSpan(
            what : Any?,
            start : Int,
            end : Int,
            flags : Int,
        ) {
            spannable.setSpan(what, start, end, flags)
        }

        override fun getSpanEnd(tag : Any?) : Int {
            return spannable.getSpanEnd(tag)
        }

        override fun getSpanFlags(tag : Any?) : Int {
            return spannable.getSpanFlags(tag)
        }

        override fun getSpanStart(tag : Any?) : Int {
            return spannable.getSpanStart(tag)
        }

        override fun <T> getSpans(
            start : Int,
            end : Int,
            type : Class<T?>?,
        ) : Array<T?>? {
            return spannable.getSpans<T?>(start, end, type)
        }

        override fun nextSpanTransition(
            start : Int,
            limit : Int,
            type : Class<*>?,
        ) : Int {
            return spannable.nextSpanTransition(start, limit, type)
        }

        val compositionStart : Int
            get() = getComposingSpanStart(spannable)

        val compositionEnd : Int
            get() = getComposingSpanEnd(spannable)

        fun setResponse(response : String?) {
            spannable.replace(0, spannable.length, response)
            Selection.selectAll(spannable)

            // have to do this because the text being edited has changed
            // this causes the keyboard to reset too, which is annoying,
            // but no way around it for a box-by-box model of input (see
            // documentation of InputMethodManager.restartInput).
            refreshInput()

            // It would be nicer if we could get away with the code
            // below, but the input method stops reporting deletion. This
            // is code we should run when the selection changes, but
            // there's no point if we're going to reset the connection
            // anyway!
            //
            // InputMethodManager imm = getInputMethodManager();
            // imm.updateSelection(
            //     targetView,
            //     0, spannable.length(),
            //     getComposingSpanStart(spannable), getComposingSpanEnd(spannable)
            // );
            // if (updateExtractedTextToken > -1) {
            //     imm.updateExtractedText(
            //         targetView,
            //         updateExtractedTextToken,
            //         getExtractedText(null, 0)
            //     );
            // }
        }
    }

    companion object {
        private val INPUT_TYPE = (InputType.TYPE_CLASS_TEXT
                or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
    }
}

