package com.example.core.util

object CloudinaryUrlHelper {
    fun optimizeForFeed(
        url: String?,
        thumbUrl: String?,
        dataSaverEnabled: Boolean
    ): String? {
        val candidate = thumbUrl?.takeIf { it.isNotBlank() } ?: url?.takeIf { it.isNotBlank() } ?: return null
        if (!candidate.contains("res.cloudinary.com") || !candidate.contains("/image/upload/")) {
            return candidate
        }
        // Avoid double-transforming URLs that already have width/quality directives
        if (candidate.contains("/upload/w_") || candidate.contains("/upload/c_")) {
            return candidate
        }
        val transform = if (dataSaverEnabled) {
            "w_320,c_limit,q_auto:low,f_auto"
        } else {
            "w_540,c_limit,q_auto:good,f_auto"
        }
        return candidate.replace("/image/upload/", "/image/upload/$transform/")
    }

    fun optimizeForDetail(url: String?, dataSaverEnabled: Boolean): String? {
        val candidate = url?.takeIf { it.isNotBlank() } ?: return null
        if (!candidate.contains("res.cloudinary.com") || !candidate.contains("/image/upload/")) {
            return candidate
        }
        if (candidate.contains("/upload/w_") || candidate.contains("/upload/c_")) {
            return candidate
        }
        val transform = if (dataSaverEnabled) {
            "w_800,c_limit,q_auto:eco,f_auto"
        } else {
            "w_1280,c_limit,q_auto:good,f_auto"
        }
        return candidate.replace("/image/upload/", "/image/upload/$transform/")
    }
}
