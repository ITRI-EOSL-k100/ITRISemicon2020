package com.itri.itrisemicon2020

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.itri.itrisemicon2020.data.ChannelRecord
import com.itri.itrisemicon2020.data.Record
//import com.itri.itrisemicon2020.data.
//import com.itri.itrisemicon2020.data.Record
import java.io.File
import java.io.FileOutputStream
import java.lang.StringBuilder

/**
 * Created by HabaCo on 2020/5/21.
 */
class FileIO(private val mContext: Context) {

    /**
     * for test 測資
     */
    fun loadRecords(defaultChannel: Boolean = false): List<ByteArray> =
        ArrayList<ByteArray>().also { dataList ->
            mContext.getExternalFilesDir("")?.run {
                repeat(5) {
                    val file = listFiles()?.first()

                    Log.i(">>>", "read file: ${file?.name}")

                    var headerCounter = 0
                    file?.forEachLine { line ->
                        if (headerCounter < 5) {
                            headerCounter++
                        } else {
                            val recordDataArray = line.split(",")
                            val value =
                                "${if (defaultChannel) "1" else ""}${recordDataArray[1].trim()}"
                            dataList.add(value.toByteArray())
                        }
                    }
                }
            }
        }

    fun saveFile(fileName: String?, dataList: List<Record>) {
        if (fileName?.isNotEmpty() == true) {
            val file = File(mContext.getExternalFilesDir(""), fileName)
            if (file.exists()) {
                file.delete()
            }
            val outputStream = FileOutputStream(file).bufferedWriter()
            try {
                outputStream.appendln("#,#")   // 1
                outputStream.appendln("#,#")   // 2
                outputStream.appendln("#,#")   // 3
                outputStream.appendln("#,#")   // 4
                outputStream.appendln("TimeInMillis,channel-1, channel-2, channel-3, channel-4, channel-5, channel-6") // 5

              /*  //// data store index start at 6
                dataList.forEach { record ->
                    if (record is ChannelRecord) {
                        outputStream.appendln("${record.time},${record.channel},${record.value}")
                    } else {
                        outputStream.appendln("${record.time},${record.value}")
                    }
                }*/
                ////6 channel data store index start at 6
                dataList.forEach { record ->
                    if (record is ChannelRecord) {
                        if (record.channel == 1){
                            outputStream.append("${record.time},${record.value},")
                        }
                        else if (record.channel == 2 ){
                            outputStream.append("${record.value},")
                        }else if(record.channel == 3){
                            outputStream.append("${record.value},")
                        }else if(record.channel == 4){
                            outputStream.append("${record.value},")
                        }else if (record.channel == 5 ){
                            outputStream.append("${record.value},")
                        }else if (record.channel == 6){
                            outputStream.appendln("${record.value}")
                        }

                    } /*else {
                        outputStream.appendln("${record.time},${record.value}")
                    }*/
                }
                outputStream.close()
                Toast.makeText(mContext, "檔案儲存位置: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(mContext, "請輸入檔名", Toast.LENGTH_SHORT).show()
        }
    }

}