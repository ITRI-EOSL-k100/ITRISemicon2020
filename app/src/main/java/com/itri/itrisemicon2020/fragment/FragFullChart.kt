package com.itri.itrisemicon2020.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.itri.itrisemicon2020.R
import com.itri.itrisemicon2020.data.ChannelRecord
import com.itri.itrisemicon2020.rms.continuousRMS
import kotlinx.android.synthetic.main.frag_full_chart.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max

/**
 * Created by HabaCo on 2020/7/20.
 */
class FragFullChart : BaseFragment() {

    private val showRange = 500    // L

    private val charts = ArrayList<LineChart?>()

    override val layoutRes = R.layout.frag_full_chart

    override fun onDataReceiveStart() {
        super.onDataReceiveStart()

        enabledScaled(charts, false)
    }

    override fun onDataChanged() {
        super.onDataChanged()

        charts.forEachIndexed { index, chart ->
            showChart(true, chart, channelDataList?.get(index + 1))
        }
    }

    override fun onDataChangedComplete() {
        super.onDataChangedComplete()

        charts.forEachIndexed { index, chart ->
            val dataList = channelDataList?.get(index + 1)
            showChart(true, chart, dataList)
        }
        enabledScaled(charts)
    }

//    override fun onThresholdChanged(threshold: Float) {
//        super.onThresholdChanged(threshold)
//
//        if (!receiving) {
//            charts.forEachIndexed { index, chart ->
//                val dataList = channelDataList?.get(index + 1)
//                showChart(true, chart, dataList, true)
//            }
//        }
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        charts.clear()
        charts.addAll(arrayOf(chart, chart2, chart3, chart4, chart5, chart6))
        initChart(charts)
    }

    private fun enabledScaled(charts: List<LineChart?>, enabled: Boolean = true) {
        charts.forEach { chart ->
            chart?.run {
                post {
                    this.setScaleEnabled(enabled)
                }
            }
        }
    }

    /**
     * 初始化 Chart 屬性
     */
    private fun initChart(charts: List<LineChart?>) {
        charts.forEach { chart ->
            //// 圖表屬性
            chart?.run {
                isAutoScaleMinMaxEnabled = false
                setDrawBorders(false)
                setPinchZoom(false)
                isHighlightPerTapEnabled = false

                legend.isEnabled = false

                xAxis.run {
                    position = XAxis.XAxisPosition.BOTTOM
                    gridColor = Color.TRANSPARENT
                    isEnabled = true
                    setLabelCount(6, true)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return String.format("%.0f", value)
                        }
                    }
                }

                axisLeft.run {
                    isEnabled = true
                    axisMaximum = 2.5f
                    axisMinimum = -2.5f
                    gridColor = Color.TRANSPARENT
                    setLabelCount(7, true)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return String.format("%.1f", value)
                        }
                    }
                }

                axisRight.isEnabled = false

                description = null

//                data = LineData(generateLineDataSet(ArrayList(), "v"))
            }
        }
    }

    private fun generateLineDataSet(entries: ArrayList<Entry>, label: String) =
        LineDataSet(entries, label).apply {
            color = Color.parseColor("#44bb66")
            this.setDrawCircles(false)
            this.setDrawValues(false)
            this.isHighlightEnabled = false
        }

    private fun showChart(
        forceUpdate: Boolean = false, chart: LineChart?, channelDataList: LinkedList<ChannelRecord>?,
        scrollToPosition: Boolean = false
    ) {
        if (!forceUpdate || channelDataList == null || channelDataList.size == 0) {
            chart?.run {
                post {
                    clear()
                }
            }
            return
        }

        syncing = true

        val dataList = channelDataList
        val subList: List<ChannelRecord> = when {
            dataList.size <= showRange -> {
                ArrayList<ChannelRecord>().also { list ->
                    val record = dataList.first()
                    repeat(showRange - dataList.size) {
                        list.add(record)
                    }
                    list.addAll(dataList)
                }
            }
            else -> dataList.subList(dataList.size - showRange, dataList.size)
        }
        val entries = ArrayList<Entry>().also { entries ->
            subList.forEachIndexed { index, record ->
                entries.add(Entry(index.toFloat() + 1, record.value / 1000f))
            }
        }
        val lineData = LineData(generateLineDataSet(entries, "v"))
        chart?.run {
            post {
                val lowX = chart.lowestVisibleX
                val xRange =
                    chart.visibleXRange //better to fix x-ranges, getVisibleXRange may be inaccurate
                val centerX = lowX + xRange / 2
//                val centerX = lineData.entryCount - showRange / 2f

                data = lineData
                invalidate()

                setScaleMinima(lineData.entryCount / showRange.toFloat(), 1f)
                if (!scrollToPosition)
                    centerViewTo(
                        max(lineData.entryCount.toFloat(), 0f),
                        0f,
                        YAxis.AxisDependency.LEFT
                    )
                else {
                    centerViewTo(centerX, 0f, YAxis.AxisDependency.LEFT)
                }

                syncing = false
            }
        }
    }
}