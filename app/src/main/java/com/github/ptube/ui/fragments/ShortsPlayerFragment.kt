package com.github.ptube.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.ptube.R
import com.github.ptube.api.obj.StreamItem
import com.github.ptube.constants.IntentData
import com.github.ptube.databinding.FragmentShortsPlayerBinding
import com.github.ptube.parcelable.PlayerData
import com.github.ptube.ui.models.HomeViewModel
import com.github.ptube.extensions.toID

class ShortsPlayerFragment : Fragment(R.layout.fragment_shorts_player) {
    private var _bindingObj: FragmentShortsPlayerBinding? = null
    private val bindingObj get() = _bindingObj!!
    private val vmHome: HomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _bindingObj = FragmentShortsPlayerBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        val bundleArgs: Bundle? = arguments
        val targetVideoId: String = bundleArgs?.getString(IntentData.videoId) ?: ""
        val passedStreamItem: StreamItem? = bundleArgs?.getParcelable(IntentData.streamItem)

        // Get the current Shorts list from ViewModel
        val homeShorts = vmHome.shorts.value ?: emptyList()
        val shortsDataList = mutableListOf<StreamItem>()

        // Construct the list:
        // If we have a passed item, ensure it is in the list
        if (passedStreamItem != null) {
            if (homeShorts.any { it.url?.toID() == passedStreamItem.url?.toID() }) {
                // If it's already in the list, just use the home list
                shortsDataList.addAll(homeShorts)
            } else {
                // If not, add it to the top (or create a list with just it + home list)
                shortsDataList.add(passedStreamItem)
                shortsDataList.addAll(homeShorts)
            }
        } else {
             shortsDataList.addAll(homeShorts)
        }
        
        var foundPosition = 0
        for (i in shortsDataList.indices) {
            val item = shortsDataList[i]
            val url = item.url
            if (url != null && url.contains(targetVideoId)) {
                foundPosition = i
                break
            }
        }

        val pagerAdapter = ShortsPagerAdapter(this, shortsDataList)
        bindingObj.shortsPager.adapter = pagerAdapter
        bindingObj.shortsPager.setCurrentItem(foundPosition, false)

        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                parentFragmentManager.beginTransaction().remove(this@ShortsPlayerFragment).commit()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bindingObj = null
    }

    class ShortsPagerAdapter(fragment: Fragment, private val list: List<StreamItem>) :
        FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = list.size

        override fun createFragment(position: Int): Fragment {
            val streamItem = list[position]
            val playerFrag = PlayerFragment()
            val playerBundle = Bundle()
            val idString = streamItem.url.orEmpty().toID()
            playerBundle.putParcelable(IntentData.playerData, PlayerData(idString))
            playerBundle.putBoolean(IntentData.alreadyStarted, false)
            playerBundle.putBoolean(IntentData.isShortsPlayer, true)
            playerFrag.arguments = playerBundle
            return playerFrag
        }
    }
}
