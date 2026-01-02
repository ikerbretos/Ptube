package com.github.ptube.ui.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.ptube.db.obj.SubscriptionGroup

class EditChannelGroupsModel : ViewModel() {
    val groups = MutableLiveData<List<SubscriptionGroup>>()
    var groupToEdit: SubscriptionGroup? = null
}
