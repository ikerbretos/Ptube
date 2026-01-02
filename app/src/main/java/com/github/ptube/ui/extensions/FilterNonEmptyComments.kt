package com.github.ptube.ui.extensions

import com.github.ptube.api.obj.Comment

fun List<Comment>.filterNonEmptyComments(): List<Comment> {
    return filter { !it.commentText.isNullOrEmpty() }
}
