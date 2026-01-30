package io.github.piyushdaiya.vaachak.core.dictionary

interface DictionaryProvider {
    suspend fun lookup(word: String): String?
}