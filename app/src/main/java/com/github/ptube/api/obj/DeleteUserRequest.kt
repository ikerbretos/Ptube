package com.github.ptube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class DeleteUserRequest(val password: String)
