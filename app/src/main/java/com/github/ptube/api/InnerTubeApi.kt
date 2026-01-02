package com.github.ptube.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

import retrofit2.http.Headers

interface InnerTubeApi {

    @Headers(
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
        "Origin: https://www.youtube.com",
        "Referer: https://www.youtube.com/"
    )
    @POST("youtubei/v1/browse?key=AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw")
    suspend fun getShorts(@Body body: InnerTubeBody): InnerTubeBrowseResponse

    @Serializable
    data class InnerTubeBody(
        val context: InnerTubeContext = InnerTubeContext(),
        val browseId: String? = "FEShorts",
        val continuation: String? = null
    )

    @Serializable
    data class InnerTubeContext(
        val client: InnerTubeClient = InnerTubeClient()
    )

    @Serializable
    data class InnerTubeClient(
        val clientName: String = "ANDROID",
        val clientVersion: String = "17.31.35",
        val androidSdkVersion: Int = 31,
        val userAgent: String = "com.google.android.youtube/17.31.35 (Linux; U; Android 12; US) gzip",
        val hl: String = "en",
        val gl: String = "US"
    )

    @Serializable
    data class InnerTubeBrowseResponse(
        val contents: Contents? = null,
        val onResponseReceivedActions: List<OnResponseReceivedAction>? = null
    )

    @Serializable
    data class OnResponseReceivedAction(
        val appendContinuationItemsAction: AppendContinuationItemsAction? = null
    )

    @Serializable
    data class AppendContinuationItemsAction(
        val continuationItems: List<RichItem>? = null
    )

    @Serializable
    data class ContinuationItem(
        val continuationItemRenderer: ContinuationItemRenderer? = null
    )

    @Serializable
    data class ContinuationItemRenderer(
        val continuationEndpoint: ContinuationEndpoint? = null
    )

    @Serializable
    data class ContinuationEndpoint(
        val continuationCommand: ContinuationCommand? = null
    )

    @Serializable
    data class ContinuationCommand(
        val token: String? = null
    )

    @Serializable
    data class Contents(
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer? = null,
        val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer? = null
    )

    @Serializable
    data class TwoColumnBrowseResultsRenderer(
        val tabs: List<Tab>? = null
    )
    
    @Serializable
    data class SingleColumnBrowseResultsRenderer(
        val tabs: List<Tab>? = null
    )

    @Serializable
    data class Tab(
        val tabRenderer: TabRenderer? = null
    )

    @Serializable
    data class TabRenderer(
        val content: TabContent? = null
    )

    @Serializable
    data class TabContent(
        val richGridRenderer: RichGridRenderer? = null
    )

    @Serializable
    data class RichGridRenderer(
        val contents: List<RichItem>? = null
    )

    @Serializable
    data class RichItem(
        val richItemRenderer: RichItemRenderer? = null,
        val continuationItemRenderer: ContinuationItemRenderer? = null
    )

    @Serializable
    data class RichItemRenderer(
        val content: RichItemContent? = null
    )

    @Serializable
    data class RichItemContent(
        val reelItemRenderer: ReelItemRenderer? = null
    )

    @Serializable
    data class ReelItemRenderer(
        val videoId: String,
        val headline: TextRun? = null,
        val thumbnail: Annotations? = null,
        val viewCountText: TextRun? = null,
        val navigationEndpoint: NavigationEndpoint? = null
    )

    @Serializable
    data class TextRun(
        val simpleText: String? = null,
        val runs: List<Run>? = null
    ) {
        val text get() = simpleText ?: runs?.joinToString("") { it.text.orEmpty() }
    }

    @Serializable
    data class Run(
        val text: String? = null
    )

    @Serializable
    data class Annotations(
        val thumbnails: List<Thumbnail>? = null
    )

    @Serializable
    data class Thumbnail(
        val url: String,
        val width: Int,
        val height: Int
    )

    @Serializable
    data class NavigationEndpoint(
        val reelWatchEndpoint: ReelWatchEndpoint? = null
    )

    @Serializable
    data class ReelWatchEndpoint(
        val videoId: String,
        val overlay: Overlay? = null
    )

    @Serializable
    data class Overlay(
       val reelPlayerOverlayRenderer: ReelPlayerOverlayRenderer? = null
    )
    
    @Serializable
    data class ReelPlayerOverlayRenderer(
        val reelPlayerHeaderSupportedRenderers: ReelPlayerHeaderSupportedRenderers? = null
    )

    @Serializable
    data class ReelPlayerHeaderSupportedRenderers(
        val reelPlayerHeaderRenderer: ReelPlayerHeaderRenderer? = null
    )

    @Serializable
    data class ReelPlayerHeaderRenderer(
       val channelTitleText: TextRun? = null,
       val channelThumbnail: Annotations? = null
    )
}
