package com.github.ptube.ui.sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.ptube.R
import com.github.ptube.databinding.DialogSubscriptionGroupsBinding
import com.github.ptube.db.DatabaseHolder
import com.github.ptube.db.obj.SubscriptionGroup
import com.github.ptube.extensions.move
import com.github.ptube.extensions.setOnDraggedListener
import com.github.ptube.ui.adapters.SubscriptionGroupsAdapter
import com.github.ptube.ui.models.EditChannelGroupsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelGroupsSheet : ExpandedBottomSheet(R.layout.dialog_subscription_groups) {
    private val channelGroupsModel: EditChannelGroupsModel by activityViewModels()

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = DialogSubscriptionGroupsBinding.bind(view)
        binding.groupsRV.layoutManager = LinearLayoutManager(context)
        val groups = channelGroupsModel.groups.value.orEmpty().toMutableList()
        val adapter = SubscriptionGroupsAdapter(groups, channelGroupsModel, parentFragmentManager)
        binding.groupsRV.adapter = adapter

        binding.newGroup.setOnClickListener {
            channelGroupsModel.groupToEdit = SubscriptionGroup("", mutableListOf(), 0)
            EditChannelGroupSheet().show(parentFragmentManager, null)
        }

        channelGroupsModel.groups.observe(viewLifecycleOwner) {
            adapter.groups = channelGroupsModel.groups.value.orEmpty().toMutableList()
            lifecycleScope.launch { adapter.notifyDataSetChanged() }
        }

        binding.confirm.setOnClickListener {
            channelGroupsModel.groups.value = adapter.groups
            channelGroupsModel.groups.value?.forEachIndexed { index, group -> group.index = index }
            CoroutineScope(Dispatchers.IO).launch {
                DatabaseHolder.Database.subscriptionGroupsDao()
                    .updateAll(channelGroupsModel.groups.value.orEmpty())
            }
            dismiss()
        }

        binding.groupsRV.setOnDraggedListener { from, to ->
            adapter.groups.move(from, to)
            adapter.notifyItemMoved(from, to)
        }
    }
}
