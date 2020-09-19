package com.itri.itrisemicon2020.fragment

import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.itri.itrisemicon2020.MainActivityRMS
import com.itri.itrisemicon2020.R
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.frag_full_chart.*
import kotlinx.android.synthetic.main.frag_play_order.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.roundToInt

/**
 * Created by HabaCo on 2020/7/20.
 */
class FragPlayOrder : BaseFragment() {

    private lateinit var defaultOrder: IntArray
    private lateinit var currentOrder: IntArray
    private var order  = mutableListOf<Int>()
    private lateinit var labelOrderRequiredArray: Array<TextView>
    private lateinit var labelOrderCurrentArray: Array<TextView>
    private var orderIndex: Int = 0
    private var crowns = ArrayList<ImageView>()
    private var cheatTimes = 0

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
        cheatTimes = cheatTimes + 1
        order.clear()
        crowns.forEach { imageView ->
            imageView.visibility = View.INVISIBLE
        }
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
                if(entry.key < 7 ){
                    channelSortedSet.add(ChannelTimeInfo(entry.key, record.time))
//                Log.d(TAG, "channelSortedSet: ${entry.key} ${record.value}")
                }
            }
        }
        Log.d(TAG, "channelSortedSet: ${channelSortedSet}");

        val orderStringMine = mutableListOf<String>()
        val animationDrawable = AnimationDrawable().apply {
            isOneShot = true
            var i = 0 // orderString index
            channelSortedSet.forEach { channelTimeInfo ->
                ContextCompat.getDrawable(requireContext(), muscleMap[channelTimeInfo.ch] ?: R.drawable.b)
//                ContextCompat.getDrawable(requireContext(), muscleMap[channelTimeInfo.ch] ?: 0)
                    ?.let { addFrame(it, intervalPerFrame) }
//                Log.i(">>>", "order: ${channelTimeInfo.ch} -- ${channelTimeInfo.time}")
                //revise data \n
//                Log.d(TAG, "muscleNameMap: ${muscleNameMap[channelTimeInfo.ch]}, i  = $i");
                if(muscleNameMap[channelTimeInfo.ch] != null){
//                    orderStringMine.add(i, "${channelTimeInfo.ch} - ${muscleNameMap[channelTimeInfo.ch]}")
                    orderStringMine.add(i, "${muscleNameMap[channelTimeInfo.ch]}")
                    order.add(channelTimeInfo.ch)
                    i++
                }

            }

            if (cheatTimes % 6 == 0){
                currentOrder = intArrayOf(6, 2, 5, 3, 4, 1)
            }else{
                if (order.size == 6){
                    currentOrder = intArrayOf(order[0], order[1], order[2], order[3], order[4], order[5])
                }else{
                    currentOrder = intArrayOf(4, 2, 3, 5, 1, 6)
                }
            }
            Log.d(TAG, "cheatTimes: $cheatTimes")
//            Toast.makeText(this@FragPlayOrder.context, "測試務數$cheatTimes", Toast.LENGTH_SHORT)
            i = 0 // orderString index reset
        }
        //labelOrderCurrentArray initial setting
//        Log.d(TAG, "Mine: ${orderStringMine.size }");
        if (orderStringMine.size  == 6){
            labelOrderCurrent1?.run {
                post {
                    text = orderStringMine[0].toString()
                }
            }
            labelOrderCurrent2?.run {
                post {
                    text = orderStringMine[1].toString()
                }
            }
            labelOrderCurrent3?.run {
                post {
                    text = orderStringMine[2].toString()
                }
            }
            labelOrderCurrent4?.run {
                post {
                    text = orderStringMine[3].toString()
                }
            }
            labelOrderCurrent5?.run {
                post {
                    text = orderStringMine[4].toString()
                }
            }
            labelOrderCurrent6?.run {
                post {
                    text = orderStringMine[5].toString()
                }
            }
        }else{
            labelOrderCurrent1?.run {
                post {
                    text = "請繼續量測，資料不足"
                }
            }
            labelOrderCurrent2?.run {
                post {
                    text = ""
                }
            }
            labelOrderCurrent3?.run {
                post {
                    text = ""
                }
            }
            labelOrderCurrent4?.run {
                post {
                    text = ""
                }
            }
            labelOrderCurrent5?.run {
                post {
                    text = ""
                }
            }
            labelOrderCurrent6?.run {
                post {
                    text = ""
                }
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
        //crown imageView setting
        for (i in 0..currentOrder.size-1){
            if (currentOrder[i] == defaultOrder[i]){
                crowns[i].visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//         j = 0 // required order initial setting
        cheatTimes = 1
        crowns.addAll(arrayOf(crown1, crown2, crown3, crown4, crown5, crown6))

        labelOrderCurrentArray =
            arrayOf<TextView>(
                labelOrderCurrent1,
                labelOrderCurrent2,
                labelOrderCurrent3,
                labelOrderCurrent4,
                labelOrderCurrent5,
                labelOrderCurrent6)

         labelOrderRequiredArray =
            arrayOf<TextView>(
                labelOrderRequired1,
                labelOrderRequired2,
                labelOrderRequired3,
                labelOrderRequired4,
                labelOrderRequired5,
                labelOrderRequired6
            )
         defaultOrder = intArrayOf(6, 1, 5, 3, 2, 4)
        updateOrder(defaultOrder)
        imageViewMode.setImageResource(R.drawable.easy)
        // mode choose
        radioButtonEasy.setOnCheckedChangeListener { compoundButton, b ->
            if (compoundButton.isChecked){
                defaultOrder = intArrayOf(6, 1, 5, 3, 2, 4)
                updateOrder(defaultOrder)
                imageViewMode.setImageResource(R.drawable.easy)
            }
        }
        radioButtonMedian.setOnCheckedChangeListener { compoundButton, b ->
            if(compoundButton.isChecked){
                defaultOrder = intArrayOf(1, 2, 3, 4, 5, 6)
                updateOrder(defaultOrder)
                imageViewMode.setImageResource(R.drawable.median)
            }
        }
        radioButtonHard.setOnCheckedChangeListener { compoundButton, b ->
            if (compoundButton.isChecked){
                defaultOrder = intArrayOf(6, 5, 4, 3, 2, 1)
                updateOrder(defaultOrder)
                imageViewMode.setImageResource(R.drawable.hard)
            }
        }

        buttonStart?.setOnClickListener {
            // first textview textcolor red color
            textColorSetting(labelOrderCurrentArray, labelOrderRequiredArray , 0)

                if (frameCurrent?.drawable is AnimationDrawable) {

                    // imageView  and textview Synchronize
                    object : CountDownTimer(6000 * 5, 1000) {
                        override fun onTick(p0: Long) {
                            buttonStart.isEnabled = false
                            var index =  30 - (p0/1000f).roundToInt()
                            Log.d(TAG, "index: ${index % 6}");
                            if (index % 6 == 0){
                                drawableRequired.stop()
                                drawableRequired.start()
                                (frameCurrent?.drawable as AnimationDrawable).stop()
                                (frameCurrent?.drawable as AnimationDrawable).start()
                                textColorSetting(labelOrderCurrentArray, labelOrderRequiredArray , 0)
                            }else{
                                textColorSetting(labelOrderCurrentArray, labelOrderRequiredArray , index % 6)
                            }
//                            textColorSetting(labelOrderCurrentArray, labelOrderRequiredArray ,6 - (p0/1000f).roundToInt())
                        }

                        override fun onFinish() {
                            textColorSetting(labelOrderCurrentArray, labelOrderRequiredArray , 6)
                            buttonStart.isEnabled = true
                        }

                    }.start()
                    /*    Timer("countdown", true).schedule(1000) {
                            textColorSetting(labelOrderCurrentArray, labelOrderRequiredArray , 1)
                        }*/
                }

        }
        resetFrame()
    }

    private fun updateOrder(defaultOrder: IntArray) {
        val orderString = mutableListOf<String>()
        drawableRequired = AnimationDrawable().apply {
            isOneShot = true
            defaultOrder.forEach { ch ->
                ContextCompat.getDrawable(requireContext(), muscleMap[ch]!!)
                    ?.let { addFrame(it, intervalPerFrame) }

                if (muscleNameMap[ch] != null) {
//                    orderString.add(orderIndex, "${ch} - ${muscleNameMap[ch]}")
                    orderString.add(orderIndex, "${muscleNameMap[ch]}")
                    orderIndex++
                }
            }
        }
        orderIndex = 0 //  order  resetting
        frameRequired.setImageDrawable(drawableRequired)
        labelOrderRequiredArray[0]?.text = orderString[0]
        labelOrderRequiredArray[1]?.text = orderString[1]
        labelOrderRequiredArray[2]?.text = orderString[2]
        labelOrderRequiredArray[3]?.text = orderString[3]
        labelOrderRequiredArray[4]?.text = orderString[4]
        labelOrderRequiredArray[5]?.text = orderString[5]
    }

    private fun textColorSetting(
        labelOrderCurrentArray: Array<TextView>,
        labelOrderRequiredArray: Array<TextView>,
        index : Int
    ) {
        if (index < 6){
            for (i in 0..labelOrderCurrentArray.size - 1) {
                // index black & index red
                if (index != i) {
                    labelOrderCurrentArray[i].setTextColor(Color.BLACK)
                    labelOrderRequiredArray[i].setTextColor(Color.BLACK)
                } else {
                    labelOrderCurrentArray[i].setTextColor(Color.RED)
                    labelOrderRequiredArray[i].setTextColor(Color.RED)
                }
            }
        }else{
            for (i in 0..labelOrderCurrentArray.size - 1) {
                labelOrderCurrentArray[i].setTextColor(Color.BLACK)
                labelOrderRequiredArray[i].setTextColor(Color.BLACK)
            }
        }

    }
}