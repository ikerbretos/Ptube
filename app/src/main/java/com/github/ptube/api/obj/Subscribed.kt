package com.github.ptube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class Subscribed(val subscribed: Boolean? = null)
