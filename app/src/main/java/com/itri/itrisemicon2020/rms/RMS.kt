package com.itri.itrisemicon2020.rms

import com.itri.itrisemicon2020.data.ChannelRecord
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * Created by HabaCo on 2020/7/23.
 */

fun Collection<ChannelRecord>.getRms(): Double {
    // 計算數列平方總和並除以數列大小
    val sum = this.sumByDouble {
        (it.value / 1000.0)    // 原始資料 / 1000 -> 電壓
            .pow(2.0)       // 平方
    } / size   // 平方後除數列大小
    return sqrt(sum)    // 平方總和取平方根
}

fun List<ChannelRecord>.getRMSList(index: Int, n: Int = 25): List<ChannelRecord> {
    val start = max(index - n, 0)   // 從 index - n 開始，若 < 0 則為 0
    val end = min(start + n, size)     // 到 index + 25 結束，若 > size 則為 size

    // 取 n .. n+25 的子數列
    return subList(start, end)
}

fun List<ChannelRecord>.continuousRMS(n: Int = 25): List<ChannelRecord> {
    return ArrayList<ChannelRecord>().also {
        // loop: index = 0 to size-1
        forEachIndexed { index, channelRecord ->
            // index 小於 n 時會取 0 to index
            // e.g.
            // 0
            // 0 1
            // 0 1 2
            //
            // index > n 時則取 index-25 to index-1
            // e.g.
            // 5 6 7 .. 29

            it.add(
                ChannelRecord(
                    channelRecord.channel
                    , channelRecord.time
                    , (getRMSList(index, n).getRms() * 1000).toInt()
                )
            )
        }
    }
}