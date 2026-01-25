package io.github.piyushdaiya.vaachak.ui.reader

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Publication
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val readiumManager: ReadiumManager
) : ViewModel() {

    private val _publication = MutableStateFlow<Publication?>(null)
    val publication = _publication.asStateFlow()

    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse = _aiResponse.asStateFlow()

    private val _isImageResponse = MutableStateFlow(false)
    val isImageResponse = _isImageResponse.asStateFlow()

    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible = _isBottomSheetVisible.asStateFlow()

    private var currentSelectedText = ""

    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            _publication.value = readiumManager.openEpubFromUri(uri)
        }
    }

    fun onTextSelected(selectedText: String) {
        currentSelectedText = selectedText
        _aiResponse.value = null
        _isBottomSheetVisible.value = true
    }

    fun onActionExplain() = viewModelScope.launch {
        _isImageResponse.value = false
        _aiResponse.value = "Thinking..."
        _aiResponse.value = aiRepository.explainContext(currentSelectedText)
    }

    fun onActionWhoIsThis() = viewModelScope.launch {
        _isImageResponse.value = false
        _aiResponse.value = "Investigating character..."

        // Extract metadata safely
        val bookTitle = _publication.value?.metadata?.title ?: "Unknown Book"
        val bookAuthor = _publication.value?.metadata?.authors?.firstOrNull()?.name ?: "Unknown Author"

        _aiResponse.value = aiRepository.whoIsThis(currentSelectedText, bookTitle, bookAuthor)
    }

    fun onActionVisualize() = viewModelScope.launch {
        _aiResponse.value = "Generating image..."

        val result = aiRepository.visualizeText(currentSelectedText)

        // Check if the repo returned the Base64 flag or the text fallback
        if (result.startsWith("BASE64_IMAGE:")) {
            _isImageResponse.value = true
            _aiResponse.value = result.removePrefix("BASE64_IMAGE:")
        } else {
            // Cloudflare failed, showing Gemini text description instead
            _isImageResponse.value = false
            _aiResponse.value = result
        }
    }

    fun dismissBottomSheet() {
        _isBottomSheetVisible.value = false
    }
}