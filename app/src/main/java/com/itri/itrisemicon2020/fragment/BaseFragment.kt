package com.itri.itrisemicon2020.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.itri.itrisemicon2020.MainActivityRMS
import com.itri.itrisemicon2020.data.ChannelRecord
import com.itri.itrisemicon2020.data.Record
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by HabaCo on 2020/7/20.
 */
abstract class BaseFragment: Fragment() {

    protected val TAG = javaClass.simpleName

    protected var thresholdStart: Float = 0f
    protected var thresholdEnd: Float = 0f
    protected var thresholdRange : Float = 0f

    protected var receiving = false

    protected var syncing = false

    var fullDataList: List<Record>? = null
        private set

    var channelDataList: HashMap<Int, LinkedList<ChannelRecord>>? = null
        private set

    open val layoutRes = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        if (layoutRes != 0)
            inflater.inflate(layoutRes, container, false)
        else
            View(requireContext())

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        thresholdStart = requireActivity().getPreferences(Context.MODE_PRIVATE).getFloat(MainActivityRMS.Pref_Threshold_Start, MainActivityRMS.Default_Threshold_Start)
        thresholdEnd = requireActivity().getPreferences(Context.MODE_PRIVATE).getFloat(MainActivityRMS.Pref_Threshold_End, MainActivityRMS.Default_Threshold_End)
        thresholdRange = requireActivity().getPreferences(Context.MODE_PRIVATE).getFloat(MainActivityRMS.Pref_Threshold_Range, MainActivityRMS.Default_Threshold_Range)
    }

    fun bindList(list: List<Record>, channelList: HashMap<Int, LinkedList<ChannelRecord>>) {
        this.fullDataList = list
        this.channelDataList = channelList
    }

    fun notifyDataChanged() {
        if (isVisible) {
            onDataChanged()
        }
    }

    open fun onSelected() {
        if (!receiving && !syncing) {
            onDataChanged()
        }
    }

    open fun onDataReceiveStart() {
        receiving = true
    }

    open fun onDataChanged() { }

    open fun onDataChangedComplete() {
        receiving = false
    }

    open fun onThresholdStartChanged(threshold: Float) {
        this.thresholdStart = threshold
        Log.d(TAG, "thresholdStart: ${threshold}");
    }

    open fun onThresholdEndChanged(threshold: Float) {
        this.thresholdEnd = threshold
    }

    open fun onThresholdRangeChanged(threshold: Float){
        this.thresholdRange = threshold
    }

}