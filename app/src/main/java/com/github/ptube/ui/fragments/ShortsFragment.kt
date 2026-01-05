package com.github.ptube.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.ptube.R
import com.github.ptube.api.obj.StreamItem
import com.github.ptube.constants.IntentData
import com.github.ptube.databinding.FragmentShortsBinding
import com.github.ptube.extensions.toID
import com.github.ptube.parcelable.PlayerData
import com.github.ptube.ui.models.HomeViewModel

class ShortsFragment : Fragment(R.layout.fragment_shorts) {
    private var _binding: FragmentShortsBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentShortsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        // Handle navigation from Home with specific Short
        arguments?.getString(IntentData.videoId)?.let { videoId ->
            val streamItem = arguments?.getSerializable(IntentData.streamItem) as? StreamItem
            if (streamItem != null) {
                val currentShorts = homeViewModel.shorts.value.orEmpty().toMutableList()
                if (currentShorts.none { it.url.orEmpty().toID() == videoId }) {
                    currentShorts.add(0, streamItem)
                    // Attempt to update ViewModel if mutable
                    (homeViewModel.shorts as? androidx.lifecycle.MutableLiveData)?.value = currentShorts
                }
            }
        }

        binding.homeRefresh.isEnabled = true
        binding.homeRefresh.setOnRefreshListener {
            refreshShorts()
        }

        homeViewModel.shorts.observe(viewLifecycleOwner) { shorts ->
            toggleLoadingIndicator(false)
            binding.nothingHere.root.isGone = !shorts.isNullOrEmpty()
            
            if (!shorts.isNullOrEmpty()) {
                try {
                    val currentAdapter = binding.baseViewPager.adapter as? ShortsPagerAdapter
                    if (currentAdapter != null && currentAdapter.itemCount != shorts.size && shorts.size > currentAdapter.itemCount) {
                        // It's an update/append
                        // Ideally we use DiffUtil or notifyDataSetChanged within adapter, but FragmentStateAdapter is tricky.
                        // Re-creating adapter resets position, which is bad.
                        // We need a Mutable FragmentStateAdapter.
                        // For simplicity in this quick fix, we'll just check if it's a NEW list or APPEND
                        // But FragmentStateAdapter doesn't support list updates easily without notifyDataSetChanged.
                        
                        // Let's make our CustomAdapter aware of list changes or just cast and update list?
                        // Actually, rebuilding adapter is the "safe" way unless we implement a mutable one.
                        // But rebuilding resets view.
                        // Let's use a Mutable implementation below.
                        (binding.baseViewPager.adapter as? ShortsPagerAdapter)?.updateList(shorts)
                    } else if (currentAdapter == null) {
                        // First load
                        val adapter = ShortsPagerAdapter(this, shorts.toMutableList())
                        binding.baseViewPager.adapter = adapter
                        binding.baseViewPager.orientation = androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
                        binding.baseViewPager.offscreenPageLimit = 1
                        
                    binding.baseViewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            
                            // 1. Load More Shorts Logic (Remote)
                            val total = binding.baseViewPager.adapter?.itemCount ?: 0
                            if (total > 0 && position >= total - 3) {
                                homeViewModel.loadMoreShorts()
                            }

                            // 2. Preload Logic (Mine)
                            val context = requireContext()
                            com.github.ptube.util.ShortsPreloader.preload(context, shorts, position)
                            
                            // 3. Send PRELOAD_VIDEO command for NEXT video (Mine)
                            if (position + 1 < shorts.size) {
                                val nextVideoId = shorts[position + 1].url.orEmpty().toID()
                                (activity as? com.github.ptube.ui.activities.MainActivity)?.runOnPlayerFragment {
                                    sendCustomCommand(
                                        androidx.media3.session.SessionCommand(com.github.ptube.enums.PlayerCommand.PRELOAD_VIDEO.name, android.os.Bundle.EMPTY),
                                        bundleOf(com.github.ptube.enums.PlayerCommand.PRELOAD_VIDEO.name to nextVideoId)
                                    )
                                    true
                                }
                            }
                        }

                        // 4. Instant Pause Logic (Remote)
                        private var lastPosition = -1
                        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                             super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                             if (positionOffset > 0.1f && position != lastPosition) {
                                  try {
                                      childFragmentManager.fragments.forEach { frag ->
                                          if (frag is com.github.ptube.ui.fragments.PlayerFragment && frag.isResumed) {
                                              frag.onInstantPauseCallback() 
                                          }
                                      }
                                      lastPosition = position 
                                  } catch (e: Exception) { e.printStackTrace() }
                             }
                             if (positionOffset == 0f) lastPosition = -1
                        }
                    })

                    // Scroll to target video if specified in arguments
                    arguments?.getString(IntentData.videoId)?.let { targetId ->
                        val index = shorts.indexOfFirst { it.url.orEmpty().toID() == targetId }
                        if (index != -1) {
                            binding.baseViewPager.setCurrentItem(index, false)
                            arguments?.remove(IntentData.videoId)
                        }
                    }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (homeViewModel.shorts.value == null) {
            refreshShorts()
        }
    }

    private fun refreshShorts() {
        toggleLoadingIndicator(true)
        homeViewModel.refreshShorts()
    }

    private fun toggleLoadingIndicator(show: Boolean) {
        binding.baseViewPager.alpha = if (show) 0.3f else 1.0f
        binding.progressBar.isGone = !show
        if (!show) binding.homeRefresh.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class ShortsPagerAdapter(fragment: Fragment, private val list: MutableList<StreamItem>) :
        FragmentStateAdapter(fragment) {
        
        fun updateList(newItems: List<StreamItem>) {
            val startSize = list.size
            // Assuming we only append
            if (newItems.size > startSize) {
                val newCount = newItems.size - startSize
                for (i in startSize until newItems.size) {
                    list.add(newItems[i])
                }
                notifyItemRangeInserted(startSize, newCount)
            } else if (newItems.size < startSize) {
                // Should not happen in append mode, but handle reset
                val oldSize = list.size
                list.clear()
                list.addAll(newItems)
                notifyDataSetChanged()
            }
        }
        
        override fun getItemCount(): Int = list.size

        override fun createFragment(position: Int): Fragment {
            val streamItem = list[position]
            val playerFrag = PlayerFragment()
            val idString = streamItem.url.orEmpty().toID()
            
            val playerBundle = bundleOf(
                IntentData.playerData to PlayerData(
                    videoId = idString,
                    thumbnailUrl = streamItem.thumbnail,
                    keepQueue = false,
                    timestamp = 0
                ),
                IntentData.alreadyStarted to false,
                IntentData.isShortsPlayer to true
            )
            playerFrag.arguments = playerBundle
            return playerFrag
        }
    }
}
