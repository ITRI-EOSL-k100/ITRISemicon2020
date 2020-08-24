package com.itri.itrisemicon2020.fragment

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.itri.itrisemicon2020.R
import kotlinx.android.synthetic.main.frag_play_order.*
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by HabaCo on 2020/7/20.
 */
class FragPlayOrder : BaseFragment() {

    /**
     * 1 -> 左 股四頭肌 b_m_b_l
     * 2 -> 左 股二頭肌 f_m_l
     * 3 -> 左 臀大肌 b_m_t_l
     * 4 -> 右 股四頭肌 b_m_b_r
     * 5 -> 右 股二頭肌 f_m_r
     * 6 -> 右 臀大肌 b_m_t_r
     */
    private var muscleNameMap = HashMap<Int, String>().apply {
        put(1, "左 股四頭肌")
        put(2, "左 股二頭肌")
        put(3, "左 臀大肌")
        put(4, "右 股四頭肌")
        put(5, "右 股二頭肌")
        put(6, "右 臀大肌")
    }

    private var muscleMap = HashMap<Int, Int>().apply {
        put(1, R.drawable.b_m_b_l)
        put(2, R.drawable.f_m_l)
        put(3, R.drawable.b_m_t_l)
        put(4, R.drawable.b_m_b_r)
        put(5, R.drawable.f_m_r)
        put(6, R.drawable.b_m_t_r)
    }

    data class ChannelTimeInfo(val ch: Int, val time: Long)

    private val intervalPerFrame = 1000
    private lateinit var drawableRequired: AnimationDrawable

    override val layoutRes = R.layout.frag_play_order

    override fun onSelected() {
        if (!receiving && channelDataList?.isNotEmpty() == true) {
            onDataChangedComplete()
        }
    }

    override fun onDataReceiveStart() {
        super.onDataReceiveStart()
        Log.i(TAG, "onDataReceiveStart")

        buttonStart?.run {
            post {
                text = "讀取資料中"
                isEnabled = false
            }
        }
    }

    override fun onDataChangedComplete() {
        super.onDataChangedComplete()

        if (context == null)
            return

        resetFrame()
    }

    private fun resetFrame() {
        val channelSortedSet = TreeSet<ChannelTimeInfo>(kotlin.Comparator { o1, o2 -> o1.time.compareTo(o2.time) })

        channelDataList?.entries?.forEach { entry ->
           /* // 啟動點 first
            entry.value.firstOrNull { record -> record.value / 1000f > thresholdStart }?.let { record ->
                // 找到啟動點，將 channel-時間加入 treeMap
                channelSortedSet.add(ChannelTimeInfo(entry.key, record.time))
            }*/
            // 啟動點 last
            entry.value.lastOrNull { record -> record.value / 1000f > thresholdStart }?.let { record ->
                // 找到啟動點，將 channel-時間加入 treeMap
                channelSortedSet.add(ChannelTimeInfo(entry.key, record.time))
            }
        }

        val orderStringBuilder = StringBuilder()
        val animationDrawable = AnimationDrawable().apply {
            isOneShot = true
            channelSortedSet.forEach { channelTimeInfo ->
                ContextCompat.getDrawable(requireContext(), muscleMap[channelTimeInfo.ch] ?: 0)
                    ?.let { addFrame(it, intervalPerFrame) }
//                Log.i(">>>", "order: ${channelTimeInfo.ch} -- ${channelTimeInfo.time}")
                if (orderStringBuilder.isNotEmpty())
                    orderStringBuilder.append("\n")
                orderStringBuilder.append("${channelTimeInfo.ch} - ${muscleNameMap[channelTimeInfo.ch]}")
            }
        }
        labelOrderCurrent?.run {
            post {
                text = orderStringBuilder.toString()
            }
        }

        frameCurrent?.run {
            post {
                setImageDrawable(animationDrawable)
            }
        }

        buttonStart?.run {
            post {
                text = "播放"
                isEnabled = true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val defaultOrder = intArrayOf(6, 1, 5, 3, 2, 4)
        val orderStringBuilder = StringBuilder()
        drawableRequired = AnimationDrawable().apply {
            isOneShot = true
            defaultOrder.forEach { ch ->
                ContextCompat.getDrawable(requireContext(), muscleMap[ch]!!)
                    ?.let { addFrame(it, intervalPerFrame) }
                if (orderStringBuilder.isNotEmpty())
                    orderStringBuilder.append("\n")
                orderStringBuilder.append("$ch - ${muscleNameMap[ch]}")
            }
        }
        frameRequired.setImageDrawable(drawableRequired)
        labelOrderRequired?.text = orderStringBuilder.toString()

        buttonStart?.setOnClickListener {
            drawableRequired.stop()
            drawableRequired.start()

            if (frameCurrent?.drawable is AnimationDrawable) {
                (frameCurrent?.drawable as AnimationDrawable).stop()
                (frameCurrent?.drawable as AnimationDrawable).start()
            }
        }

    }
}