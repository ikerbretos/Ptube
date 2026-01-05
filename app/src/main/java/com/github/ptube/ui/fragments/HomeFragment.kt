package com.github.ptube.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.github.ptube.R
import com.github.ptube.api.MediaServiceRepository
import com.github.ptube.api.TrendingCategory
import com.github.ptube.api.obj.Playlists
import com.github.ptube.api.obj.StreamItem
import com.github.ptube.constants.PreferenceKeys
import com.github.ptube.constants.PreferenceKeys.HOME_TAB_CONTENT
import com.github.ptube.databinding.FragmentHomeBinding
import com.github.ptube.db.obj.PlaylistBookmark
import com.github.ptube.helpers.PreferenceHelper
import com.github.ptube.ui.activities.SettingsActivity
import com.github.ptube.ui.adapters.CarouselPlaylist
import com.github.ptube.ui.adapters.CarouselPlaylistAdapter
import com.github.ptube.ui.adapters.VideoCardsAdapter
import com.github.ptube.ui.models.HomeViewModel
import com.github.ptube.ui.models.SubscriptionsViewModel
import com.github.ptube.ui.models.TrendsViewModel
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar


class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val subscriptionsViewModel: SubscriptionsViewModel by activityViewModels()
    private val trendsViewModel: TrendsViewModel by activityViewModels()

    private val trendingAdapter = VideoCardsAdapter()
    private val feedAdapter = VideoCardsAdapter() // Removed fixed width so it takes match_parent
    private val watchingAdapter = VideoCardsAdapter(columnWidthDp = 250f)
    private val bookmarkAdapter = CarouselPlaylistAdapter()
    private val playlistAdapter = CarouselPlaylistAdapter()
    private val shortsAdapter = com.github.ptube.ui.adapters.ShortsAdapter()

    // State for mixed feed
    private var currentFeed: List<StreamItem> = emptyList()
    private var currentShorts: List<StreamItem> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.bookmarksRV.layoutManager = CarouselLayoutManager(UncontainedCarouselStrategy())
        binding.playlistsRV.layoutManager = CarouselLayoutManager(UncontainedCarouselStrategy())

        val bookmarksSnapHelper = CarouselSnapHelper()
        bookmarksSnapHelper.attachToRecyclerView(binding.bookmarksRV)

        val playlistsSnapHelper = CarouselSnapHelper()
        playlistsSnapHelper.attachToRecyclerView(binding.playlistsRV)

        // Main Mixed Feed Setup
        val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 2)
        gridLayoutManager.spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val item = feedAdapter.currentList.getOrNull(position)
                return when {
                    item?.type == StreamItem.TYPE_HEADER -> 2 // Header takes full width
                    item?.isShort == true -> 1 // Shorts take half width (2 per row)
                    else -> 2 // Videos take full width
                }
            }
        }
        binding.featuredRV.layoutManager = gridLayoutManager
        binding.featuredRV.adapter = feedAdapter
        
        binding.trendingRV.adapter = trendingAdapter
        // binding.shortsRV.adapter = shortsAdapter // No longer using separate shorts RV
        binding.bookmarksRV.adapter = bookmarkAdapter
        binding.playlistsRV.adapter = playlistAdapter
        binding.playlistsRV.adapter?.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                if (itemCount == 0) {
                    binding.playlistsRV.isGone = true
                    binding.playlistsTV.isGone = true
                }
            }
        })
        // binding.watchingRV.adapter = watchingAdapter // Removed as per request

        with(homeViewModel) {
            trending.observe(viewLifecycleOwner, ::showTrending)
            feed.observe(viewLifecycleOwner, ::showFeed)
            shorts.observe(viewLifecycleOwner, ::showShorts)
            bookmarks.observe(viewLifecycleOwner, ::showBookmarks)
            playlists.observe(viewLifecycleOwner, ::showPlaylists)
            // continueWatching.observe(viewLifecycleOwner, ::showContinueWatching) // Removed as per request
            isLoading.observe(viewLifecycleOwner, ::updateLoading)
        }

        binding.featuredTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_subscriptionsFragment)
        }

        binding.shortsTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_shortsFragment)
        }

        // binding.watchingTV.setOnClickListener { // Removed
        //    findNavController().navigate(R.id.action_homeFragment_to_watchHistoryFragment)
        // }

        binding.trendingTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_trendsFragment)
        }

        binding.playlistsTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_libraryFragment)
        }

        binding.bookmarksTV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_libraryFragment)
        }

        binding.chipsGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds.first()
                val chip = group.findViewById<com.google.android.material.chip.Chip>(chipId)
                if (chip != null) {
                    Snackbar.make(binding.root, "Filtrando por: ${chip.text}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.refresh.setOnRefreshListener {
            binding.refresh.isRefreshing = true
            fetchHomeFeed()
        }

        binding.refreshButton.setOnClickListener {
            fetchHomeFeed()
        }

        binding.changeInstance.setOnClickListener {
            redirectToIntentSettings()
        }

        // Hide headers to match YouTube's infinite scroll look
        binding.trendingTV.visibility = View.GONE
        binding.featuredTV.visibility = View.GONE
        binding.trendingRV.visibility = View.GONE // Hide trending RV
        binding.shortsTV.visibility = View.GONE
        binding.shortsRV.visibility = View.GONE // Hide independent shorts RV
    }

    override fun onResume() {
        super.onResume()

        // Avoid re-fetching when re-entering the screen if it was loaded successfully, except when
        // the value of trending region has changed
        val isTrendingRegionChanged = homeViewModel.trending.value?.let {
            it.second.region != PreferenceHelper.getTrendingRegion(requireContext())
        } == true

        if (homeViewModel.loadedSuccessfully.value == false || isTrendingRegionChanged) {
            fetchHomeFeed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchHomeFeed() {
        binding.nothingHere.isGone = true
        val defaultItems = resources.getStringArray(R.array.homeTabItemsValues)
        val visibleItems = PreferenceHelper.getStringSet(HOME_TAB_CONTENT, defaultItems.toSet()).toMutableSet()
        // Ensure shorts is always included even for existing users who already have a saved preference set
        visibleItems.add("shorts")

        homeViewModel.loadHomeFeed(
            context = requireContext(),
            subscriptionsViewModel = subscriptionsViewModel,
            visibleItems = visibleItems,
            onUnusualLoadTime = ::showChangeInstanceSnackBar
        )
    }

    private fun showTrending(trends: Pair<TrendingCategory, TrendsViewModel.TrendingStreams>?) {
        if (trends == null) return
        val (category, trendingStreams) = trends
        val region = PreferenceHelper.getTrendingRegion(requireContext())
        trendsViewModel.setStreamsForCategory(
            category,
            TrendsViewModel.TrendingStreams(region, trendingStreams.streams)
        )
        
        // Critical Fix: Add trending videos to the mixed feed so normal videos appear!
        val newVideos = trendingStreams.streams.filter { !it.isShort }
        if (newVideos.isNotEmpty()) {
            val combined = (currentFeed + newVideos).distinctBy { it.url }
            currentFeed = combined
            updateMixedFeed()
        }
    }

    private fun showFeed(streamItems: List<StreamItem>?) {
        if (streamItems == null) return
        // Merge with existing (which might contain trending)
        val combined = (currentFeed + streamItems).distinctBy { it.url }
        currentFeed = combined
        updateMixedFeed()
    }

    private fun showShorts(shorts: List<StreamItem>?) {
        if (shorts == null) return
        currentShorts = shorts.map { it.copy(isShort = true) } // Ensure isShort is true
        updateMixedFeed()
    }

    private fun updateMixedFeed() {
        // Strict Sectioned Logic:
        // [Header: Shorts] -> 4 Shorts
        // [Header: Videos] -> 4 Videos
        // Loop
        val mixedList = mutableListOf<StreamItem>()
        var shortIndex = 0
        var videoIndex = 0
        
        // Initial set
        appendSection(mixedList, shortIndex, videoIndex)
        // Advance indices
        shortIndex = (shortIndex + 4) % (if(currentShorts.isNotEmpty()) currentShorts.size else 1)
        videoIndex = (videoIndex + 4) % (if(currentFeed.isNotEmpty()) currentFeed.size else 1)
        
        makeVisible(binding.featuredRV)
        feedAdapter.submitList(mixedList)
        
        // Infinite Scroll - Real Network Fetch
        binding.featuredRV.clearOnScrollListeners() 
        binding.featuredRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) { // Reached bottom
                   // Trigger network fetch
                   homeViewModel.loadMore(subscriptionsViewModel)
                   
                   // FALLBACK: Immediately append a looped section to ensure user never sees "nothing".
                   // If network returns new items, they will be mixed in later.
                   // This guarantees "Infinite" feeling even if API is slow/empty.
                   val currentList = feedAdapter.currentList.toMutableList()
                   appendSection(currentList, (currentList.size * 3) % (currentShorts.size.coerceAtLeast(1)), (currentList.size * 5) % (currentFeed.size.coerceAtLeast(1)))
                   feedAdapter.submitList(currentList)
                }
            }
        })
    }

    private fun appendSection(targetList: MutableList<StreamItem>, startShortIndex: Int, startVideoIndex: Int) {
        // 1. Shorts Section
        if (currentShorts.isNotEmpty()) {
            targetList.add(StreamItem(type = StreamItem.TYPE_HEADER, title = "Shorts"))
            var addedShorts = 0
            var sIdx = startShortIndex
            while (addedShorts < 4) {
                 if (sIdx >= currentShorts.size) sIdx = 0 // Loop internal
                 targetList.add(currentShorts[sIdx].copy(isShort = true))
                 sIdx++
                 addedShorts++
            }
        }
        
        // 2. Videos Section
        if (currentFeed.isNotEmpty()) {
            // Deterministic Titles
            val titles = listOf(
                "Novedades",
                "Tendencias",
                "Noticias", 
                "Videojuegos",
                "Música",
                "Películas",
                "Aprendizaje"
            )
            // Use a static counter or derive from list size to cycle
            val titleIndex = (targetList.size / 8) % titles.size // Approx every 8 items is a new section
            val title = titles[titleIndex]
            
            targetList.add(StreamItem(type = StreamItem.TYPE_HEADER, title = title))
            var addedVideos = 0
            var vIdx = startVideoIndex
            while (addedVideos < 4) {
                 if (vIdx >= currentFeed.size) vIdx = 0 // Loop internal
                 targetList.add(currentFeed[vIdx])
                 vIdx++
                 addedVideos++
            }
        }
    }

    private fun showBookmarks(bookmarks: List<PlaylistBookmark>?) {
        if (bookmarks == null) return

        makeVisible(binding.bookmarksTV, binding.bookmarksRV)
        bookmarkAdapter.submitList(bookmarks.map { bookmark ->
            CarouselPlaylist(
                id = bookmark.playlistId,
                title = bookmark.playlistName,
                thumbnail = bookmark.thumbnailUrl
            )
        })
    }

    private fun showPlaylists(playlists: List<Playlists>?) {
        if (playlists == null) return

        makeVisible(binding.playlistsRV, binding.playlistsTV)
        playlistAdapter.submitList(playlists.map { playlist ->
            CarouselPlaylist(
                id = playlist.id!!,
                thumbnail = playlist.thumbnail,
                title = playlist.name
            )
        })
    }

    private fun showContinueWatching(unwatchedVideos: List<StreamItem>?) {
        if (unwatchedVideos == null) return

        // makeVisible(binding.watchingRV, binding.watchingTV) // Removed
        // watchingAdapter.submitList(unwatchedVideos) // Removed
    }

    private fun updateLoading(isLoading: Boolean) {
        if (isLoading) {
            showLoading()
        } else {
            hideLoading()
        }
    }

    private fun showLoading() {
        binding.progress.isVisible = !binding.refresh.isRefreshing
        binding.nothingHere.isVisible = false
        binding.scroll.alpha = 0.3f
    }

    private fun hideLoading() {
        binding.progress.isVisible = false
        binding.refresh.isRefreshing = false

        val hasContent = homeViewModel.loadedSuccessfully.value == true
        if (hasContent) {
            showContent()
        } else {
            showNothingHere()
        }
        binding.scroll.alpha = 1.0f
    }

    private fun showNothingHere() {
        binding.nothingHere.isVisible = true
        binding.scroll.isVisible = false
    }

    private fun showContent() {
        binding.nothingHere.isVisible = false
        binding.scroll.isVisible = true
    }

    private fun showChangeInstanceSnackBar() {
        val root = _binding?.root ?: return
        Snackbar
            .make(root, R.string.suggest_change_instance, Snackbar.LENGTH_LONG)
            .apply {
                setAction(R.string.change) {
                    redirectToIntentSettings()
                }
                show()
            }
    }

    private fun redirectToIntentSettings() {
        val settingsIntent = Intent(context, SettingsActivity::class.java).apply {
            putExtra(SettingsActivity.REDIRECT_KEY, SettingsActivity.REDIRECT_TO_INTENT_SETTINGS)
        }
        startActivity(settingsIntent)
    }

    private fun makeVisible(vararg views: View) {
        views.forEach { it.isVisible = true }
    }
}
