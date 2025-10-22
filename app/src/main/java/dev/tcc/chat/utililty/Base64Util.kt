package dev.tcc.chat.utililty

import android.util.Base64

object Base64Util {

    fun encode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun decode(string: String): ByteArray {
        return Base64.decode(string, Base64.NO_WRAP)
    }
}