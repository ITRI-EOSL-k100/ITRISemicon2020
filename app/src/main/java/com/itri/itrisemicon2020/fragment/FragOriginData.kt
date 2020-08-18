package com.itri.itrisemicon2020.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.itri.itrisemicon2020.R
import com.itri.itrisemicon2020.data.ChannelRecord
import com.itri.itrisemicon2020.rms.getRMSList
import com.itri.itrisemicon2020.rms.getRms
import kotlinx.android.synthetic.main.frag_origin_data.*
import kotlin.math.max

/**
 * Created by HabaCo on 2020/7/20.
 */
class FragOriginData : BaseFragment() {

    private val showRange = 1000
    private val rmsFreq = 20        // RMS n

    private var channelSelection1 = 1
    private var channelSelection2 = 1

    private var chart1syncing = false
    private var chart2syncing = false

    private var themeColor: Int = Color.parseColor("#44bb66")
    private var pinStart: Drawable? = null
    private var pinEnd: Drawable? = null

    override val layoutRes = R.layout.frag_origin_data

    override fun onAttach(context: Context) {
        super.onAttach(context)

        themeColor = ContextCompat.getColor(context, R.color.colorPrimary)
        pinStart = ContextCompat.getDrawable(context, R.drawable.pin)
        pinEnd = ContextCompat.getDrawable(context, R.drawable.pin_end)
    }

    override fun onDataChanged() {
        super.onDataChanged()

        showChart(chartOrigin, channelSelection1)
        showChart(chartOrigin2, channelSelection2)
    }

    override fun onDataChangedComplete() {
        super.onDataChangedComplete()

        //// 關閉接收資料，顯示圖表
        showChart(chartOrigin, channelSelection1)
        showChart(chartOrigin2, channelSelection2)
    }

    override fun onThresholdStartChanged(threshold: Float) {
        super.onThresholdStartChanged(threshold)

        if (!receiving) {
            showChart(chartOrigin, channelSelection1)
            showChart(chartOrigin2, channelSelection2)
        }
    }

    override fun onThresholdEndChanged(threshold: Float) {
        super.onThresholdEndChanged(threshold)

        if (!receiving) {
            showChart(chartOrigin, channelSelection1)
            showChart(chartOrigin2, channelSelection2)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initChart(chartOrigin)
        initChart(chartOrigin2)

        channelGroup1?.setOnCheckedChangeListener { _, checkedId ->
            channelSelection1 = when (checkedId) {
                R.id.group1Channel1 -> 1
                R.id.group1Channel2 -> 2
                R.id.group1Channel3 -> 3
                R.id.group1Channel4 -> 4
                R.id.group1Channel5 -> 5
                R.id.group1Channel6 -> 6
                else -> 1
            }
            if (!receiving)
                showChart(chartOrigin, channelSelection1)
        }

        channelGroup2?.setOnCheckedChangeListener { _, checkedId ->
            channelSelection2 = when (checkedId) {
                R.id.group2Channel1 -> 1
                R.id.group2Channel2 -> 2
                R.id.group2Channel3 -> 3
                R.id.group2Channel4 -> 4
                R.id.group2Channel5 -> 5
                R.id.group2Channel6 -> 6
                else -> 1
            }
            if (!receiving)
                showChart(chartOrigin2, channelSelection2)
        }
    }

    /**
     * 初始化 Chart 屬性
     */
    private fun initChart(chart: LineChart?) {
        //// 圖表屬性
        chart?.run {
            isAutoScaleMinMaxEnabled = false
            setDrawBorders(true)
            setPinchZoom(false)
            setDragEnabled(true) //启用/禁用拖动（平移）图表。
//            setScaleEnabled(false) //启用/禁用缩放图表上的两个轴。
            setTouchEnabled(true) //启用/禁用与图表的所有可能的触摸交互。
            isHighlightPerTapEnabled = false

            legend.isEnabled = false

            xAxis.run {
                position = XAxis.XAxisPosition.BOTTOM
                gridColor = Color.TRANSPARENT
                isEnabled = true
                axisMinimum = 1f
                axisMaximum = showRange.toFloat()
                setLabelCount(11, true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%.0f", value)
                    }
                }
                setMaxVisibleValueCount(showRange + 1)
            }

            axisLeft.run {
                isEnabled = true
                axisMaximum = 2.5f
                axisMinimum = -2.5f
                gridColor = Color.TRANSPARENT
                setLabelCount(11, true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%.1f", value)
                    }
                }
            }

            axisRight.isEnabled = false

            description = null
        }
    }

    private fun generateLineDataSet(entries: ArrayList<Entry>, label: String = "v") =
        LineDataSet(entries, label).apply {
            color = themeColor
            setDrawCircles(false)
            setDrawValues(false)
            setDrawIcons(true)
            isHighlightEnabled = false
        }

    private fun showChart(chart: LineChart?, channelDataIndex: Int) {
        val channelDataList = this.channelDataList?.get(channelDataIndex)

        if (channelDataList == null || channelDataList.size == 0) {
            return
        }

        if (chart == chartOrigin) {
            if (chart1syncing)
                return
            else
                chart1syncing = true
        }

        if (chart == chartOrigin2) {
            if (chart2syncing)
                return
            else chart2syncing = true
        }

        syncing = true

        val dataList = channelDataList
        val subList: List<ChannelRecord> = when {
            // 為了計算前 n-1 個數的 RMS Data，需保留額外 n-1 個數列長度
            // ...
            // 例如數列 1-100 n=25
            // 會額外保留 24 個數做運算，因此運算 RMS 的數列會是 1 ~ 124
            dataList.size < showRange + (rmsFreq - 1) -> {
                ArrayList<ChannelRecord>().also { list ->
                    val record = dataList.first()
                    repeat(showRange + (rmsFreq - 1) - dataList.size) {
                        list.add(record)
                    }
                    list.addAll(dataList)
                }
            }
            else -> dataList.subList(dataList.size - (showRange + (rmsFreq - 1)), dataList.size)
        }
        val max = subList.maxBy {
            it.value
        }
        //text print

        if (chart == chartOrigin){
            val min = 0.03f
            val maxValue = (max?.value)!!
            val SNR = 20 * Math.log10((maxValue / min).toDouble())
//            Log.d(TAG, "maxValue: $maxValue")
            SNR_original?.run {
                post {
                    text = String.format("SNR = %.3f", SNR)
                }
            }
        }else if(chart == chartOrigin2){
            val min = 0.01f
            val maxValue = (max?.value!!)
            val SNR = 20 * Math.log10((maxValue / min).toDouble())
            SNR_compensate?.run {
                post {
                    text = String.format("SNR = %.3f", SNR)
                }
            }
        }


        var maxRMS = 0.0
        val entries = ArrayList<Entry>().also { entries ->
            var hasStartPin = false
            var durationTime = 0
            subList.forEachIndexed { index, record ->
                // RMS 數列計算完成後將 index 做位移從第 n 個開始 (x=1)
                // ...
                // 例如數列 1-100 n=25
                // 會額外保留 24 個數做運算，因此運算 RMS 的數列會是 1 ~ 124
                // RMS 運算完畢後將 index-(n-1) 做位移 -> -24 ~ 99 (java 陣列空間為 0~n-1，所以尾數是 99)

                // 位移 index，x 從第 n 個數開始為 1
                val entryIndex = index + 1 - (rmsFreq - 1f)

                // 隱藏前 n-1 個僅用於計算 RMS 的數列
                if (entryIndex > 0) {
                    var value = record.value / 1000f
                    val rms = subList.getRMSList(index).getRms()

                    maxRMS = max(maxRMS, rms)

                    if (chart == chartOrigin) {
                        textMaxRms?.run {
                            post {
                                text = String.format("%.4f", maxRMS)
                            }
                        }
                    } else if (chart == chartOrigin2) {
                        textMaxRms2?.run {
                            post {
                                text = String.format("%.4f", maxRMS)
                            }
                        }
                    }



                    if(chart == chartOrigin){
                        val entry =
                            if (!hasStartPin && rms >= thresholdStart - 0.15) {
                                hasStartPin = true
                                Entry(entryIndex, value, pinStart)
                            } else if (hasStartPin && rms <= thresholdEnd - 0.05) {
                                hasStartPin = false
                                Entry(entryIndex, value, pinEnd)
                            } else {
                                Entry(entryIndex, value)
                            }
                        entries.add(entry)
                    }else if (chart == chartOrigin2){
                        if (value > -0.2 && value < 0.19){
                            value = value / 2f
                        }
                        val entry =
                            if (!hasStartPin && rms >= thresholdStart ) {
                                hasStartPin = true
                                durationTime = 1
                                Entry(entryIndex, value, pinStart)
                            } else if (hasStartPin && rms <= thresholdEnd && durationTime > 300) {
                                hasStartPin = false
                                durationTime = 0
                                Entry(entryIndex, value, pinEnd)
                            } else {
                                durationTime ++
                                Entry(entryIndex, value)
                            }
                        entries.add(entry)
                    }
                }
            }
        }
        val lineData = LineData(generateLineDataSet(entries))
        chart?.run {
            post {
                data = lineData
                invalidate()

                if (chart == chartOrigin) {
                    chart1syncing = false
                }

                if (chart == chartOrigin2) {
                    chart2syncing = false
                }

                syncing = false
            }
        }
    }
}