package com.github.ptube.repo

import com.github.ptube.api.RetrofitInstance
import com.github.ptube.api.obj.StreamItem
import com.github.ptube.helpers.PreferenceHelper

class PipedAccountFeedRepository : FeedRepository {
    override suspend fun getFeed(
        forceRefresh: Boolean,
        onProgressUpdate: (FeedProgress) -> Unit
    ): List<StreamItem> {
        val token = PreferenceHelper.getToken()

        return RetrofitInstance.authApi.getFeed(token)
    }
}
