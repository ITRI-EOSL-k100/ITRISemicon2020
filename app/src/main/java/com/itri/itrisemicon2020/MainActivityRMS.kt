package com.itri.itrisemicon2020

import android.app.Dialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.itri.itrisemicon2020.data.ChannelRecord
import com.itri.itrisemicon2020.fragment.BaseFragment
import com.itri.itrisemicon2020.fragment.FragFullChart
import com.itri.itrisemicon2020.fragment.FragOriginData
import com.itri.itrisemicon2020.fragment.FragPlayOrder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.buttonEmail
import kotlinx.android.synthetic.main.activity_main.buttonSaveFile
import kotlinx.android.synthetic.main.activity_main.editEmail
import kotlinx.android.synthetic.main.activity_main.editFileName
import kotlinx.android.synthetic.main.activity_main.progressScanning
import kotlinx.android.synthetic.main.activity_main.textDeviceInfo
import kotlinx.android.synthetic.main.activity_main.toggleRecord
import kotlinx.android.synthetic.main.activity_main2.*
import java.io.File
import java.util.*
import kotlin.concurrent.thread
import kotlin.concurrent.timer

class MainActivityRMS : AppCompatActivity() {

    companion object {
        const val Pref_Threshold_Start = "threshold-Start"
        const val Default_Threshold_Start = 0.0446f

        const val Pref_Threshold_End = "threshold-End"
        const val Default_Threshold_End = 0.001f

        const val Pref_Threshold_Range = "threshold-Range"
        const val Default_Threshold_Range = 0.2f

        const val Pref_Duration = "duration"
        const val Default_Duration = 50f
    }

    //// 節省時間，使用過時的 ProgressDialog
    private var progressDialog: ProgressDialog? = null

    //// 藍芽掃描 tool
    private val bleScanner: BleScanner by lazy {
        if (Build.VERSION.SDK_INT >= 21)
            BleScanner.BleScannerAPI21(this).apply {
                enableDebug()
            }
        else
            BleScanner.BleScannerAPI18(this).apply {
                enableDebug()
            }
    }

    //// 藍芽掃描計時
    private var stopScanningCounter = 0

    //// 藍芽掃描裝置清單
    private var bleList: ArrayList<BluetoothDevice> = ArrayList()
    private var bleListDialogAdapter: ArrayAdapter<BluetoothDevice>? = null

    //// 藍芽連線 tool
    private val bleConnector: BleConnector by lazy {
        BleConnector(this)
    }

    //// 測量資料
    private val fullDataList = LinkedList<ChannelRecord>()
    private val channelDataList = HashMap<Int, LinkedList<ChannelRecord>>()

    //// 分頁
    private val fragments = arrayOf(FragOriginData(), FragFullChart(), FragPlayOrder())
    private var currentPage: BaseFragment? = null

    //// 藍芽資料串接 Buffer
    private var serialAsciiBuffer = StringBuilder()

    //// 開始記錄時間
    private var timeStartRecord = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        bleScanner.onDeviceFoundDefault = { device, _, _ ->
            if (device.name?.contains("LAIRD") == true) {
                if (!bleList.contains(device)) {
                    runOnUiThread {
                        bleList.add(device)
                        bleListDialogAdapter?.notifyDataSetChanged()
                    }
                }
            }
            Log.i(
                ">>>",
                "scan device(${device.name ?: "N/A"}): ${device.address}"
            )
        }

        bleConnector.run {
            onConnecting = {
                toggleRecord?.isChecked = false
                toggleRecord?.isEnabled = false
                progressDialog?.dismiss()
                progressDialog = ProgressDialog.show(this@MainActivityRMS, null, "連線中，請稍候")
            }
            onServiceDiscovering = {
                runOnUiThread {
                    bleConnector.selectedDevice()?.run {
                        textDeviceInfo?.text = String.format("%s\n%s", name, address)
                        textDeviceInfo?.visibility = View.VISIBLE
                    }
                    toggleRecord?.isChecked = false
                    toggleRecord?.isEnabled = false
                    progressDialog?.dismiss()
                    progressDialog = ProgressDialog.show(this@MainActivityRMS, null, "正在搜尋服務 UUID")
                }
            }
            onServiceDiscoverCompleted = {
                runOnUiThread {
                    toggleRecord?.isChecked = false
                    toggleRecord?.isEnabled = true
                    progressDialog?.dismiss()
                    Toast.makeText(this@MainActivityRMS, "搜尋完成", Toast.LENGTH_SHORT).show()
                }
            }
            onDisconnected = {
                runOnUiThread {
                    textDeviceInfo?.visibility = View.INVISIBLE
                    toggleRecord?.isChecked = false
                    toggleRecord?.isEnabled = false
                    progressDialog?.dismiss()
                }
            }
            onNotificationChanged = {
                runOnUiThread {
                    toggleRecord?.isEnabled = true
                }

                if (toggleRecord?.isChecked == false) {
                    //// 關閉接收資料，顯示圖表
                    fragments.forEach {
                        it.onDataChangedComplete()
                    }
                } else {
                    fragments.forEach {
                        it.onDataReceiveStart()
                    }
                }
            }
            onDataIn = { bytes ->
                if (toggleRecord?.isEnabled == true) {

                    // data bytes in to string
                    val bytes2Text = String(bytes)

                    val textArr = bytes2Text.split("\n")
                    if (serialAsciiBuffer.isNotEmpty()) {
                        serialAsciiBuffer.append(textArr[0])
                    }
                    val preText = serialAsciiBuffer.toString()

                    if (textArr.size > 1) {
                        //// 檢測至分隔符號
                        serialAsciiBuffer.clear()
                        serialAsciiBuffer.append(textArr.last())

                        val tempList = ArrayList<ChannelRecord>()

                        if (preText.isNotEmpty() && preText.length == 5) {
                            val dataValue = getData(preText)

                            tempList.add(
                                ChannelRecord(
                                    dataValue[0],
                                    timeStartRecord + fullDataList.size,
                                    dataValue[1]
                                )
                            )
                        }
                        for (i in 1 until textArr.size - 1) {
                            if (textArr[i].length == 5) {
                                val dataValue = getData(textArr[i])

                                tempList.add(
                                    ChannelRecord(
                                        dataValue[0],
                                        timeStartRecord + fullDataList.size + tempList.size,
                                        dataValue[1]
                                    )
                                )
                            }
                        }
//                        Log.d("fullDataList", "fullDataList: ${fullDataList.size}");//10s 5186 筆 data
                        fullDataList.addAll(tempList)

                        if (fullDataList.size > 25) {
                            for (record in tempList) {
                                if (channelDataList.containsKey(record.channel)) {
                                    channelDataList[record.channel]!!.add(record)
                                } else {
                                    channelDataList[record.channel] = LinkedList<ChannelRecord>().also {
                                        it.add(record)
                                    }
                                }
                            }
                            Log.d("MainActivity", "${channelDataList.keys}");
                            currentPage?.notifyDataChanged()
                        }
                    }
                }
            }
        }

        initPager()

        toggleRecord?.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.isEnabled = false

            if (isChecked) {
                fullDataList.clear()
                bleConnector.enableDataIncome()
                buttonSaveFile.isEnabled = false
            } else {
                bleConnector.disableDataIncome()
                buttonSaveFile.isEnabled = true
            }
        }

        buttonSaveFile?.setOnClickListener {
            saveFile()
        }

        buttonEmail?.setOnClickListener {
            email()
        }

//        buttonToggle?.setOnClickListener {
//            textConsole?.visibility = if (textConsole?.isShown == true) View.GONE else View.VISIBLE
//        }

        /*buttonSaveFile?.isEnabled = true
        // 讀檔 (測試資料)
        thread {
            fragments.forEach {
                it.onDataReceiveStart()
            }
                FileIO(this).loadRecords().forEach { bytes ->
                    Thread.sleep(5)
//                if (toggleRecord?.isChecked == true) {
                    val bytes2Text = String(bytes)

                    if (bytes2Text.length < 6) {

                        val dataValue = getData(bytes2Text)

                        val record = ChannelRecord(
                            dataValue[0],
                            fullDataList.size.toLong(),
                            dataValue[1]
                        )

                        fullDataList.add(record)

                        if (fullDataList.size > 25) {
                            if (channelDataList.containsKey(record.channel)) {
                                channelDataList[record.channel]!!.add(record)
                            } else {
                                channelDataList[record.channel] = LinkedList<ChannelRecord>().also {
                                    it.add(record)
                                }
                            }

                            currentPage?.notifyDataChanged()
                        }
                    }
//                }
                }
            fragments.forEach {
                it.onDataChangedComplete()
            }
        }*/
    }

    /**
     * 分析訊號
     * @param dataInText 訊號資料 (5)
     * @return IntArray of [0]= channel(1), [1]= value(4)
     */
    private fun getData(dataInText: String): IntArray {
        val channel = dataInText[0].toInt() - 48    // ascii value
        val value = dataInText.substring(1).toInt() - 1500  // -1500 = -1.5v
        return intArrayOf(channel, value)
    }

    private fun initPager() {
        pager.setOnTouchListener { _, _ -> true }


        pager.adapter = object : FragmentStateAdapter(this) {

            override fun getItemCount() = fragments.size

            override fun createFragment(position: Int) = fragments[position].also {
                it.bindList(fullDataList, channelDataList)
            }
        }
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = fragments[position]
                currentPage?.onSelected()
            }
        })

        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.setIcon(
                when (position) {
                    0 -> R.drawable.tab_origin_data
                    1 -> R.drawable.tab_full_chart
                    2 -> R.drawable.tab_play_order
                    else -> 0
                }
            )
        }.attach()

    }

    /**
     * 存檔
     */
    private fun saveFile() {
        FileIO(this).saveFile(editFileName?.text?.toString(), fullDataList)
    }

    private fun email() {
        val mail = editEmail.text.toString()
        if (mail.isEmpty()) {
            Toast.makeText(this, "請輸入信箱", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = editFileName.text.toString()
        val file = File(getExternalFilesDir(""), fileName)
        if (fileName.isNotEmpty()) {
            if (!file.exists()) {
                Toast.makeText(this, "找不到該檔案", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            Toast.makeText(this, "請輸入檔名指定附件", Toast.LENGTH_SHORT).show()
            return
        }

        val uri =
            if (Build.VERSION.SDK_INT >= 24)
                FileProvider.getUriForFile(this, "$packageName.provider", file)
            else
                Uri.fromFile(file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(mail))
            putExtra(Intent.EXTRA_SUBJECT, "監測記錄")
            putExtra(Intent.EXTRA_TEXT, "監測記錄")
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        startActivityForResult(intent, 123)
    }

    override fun onDestroy() {
        super.onDestroy()

        bleConnector.disconnect()
    }

    /**
     * # 掃描按鈕點擊
     *
     *      1. 取消目前連線
     *      2. 開始掃描
     *
     * @throws BleScanner.BluetoothNotEnabledException 藍芽未開啟
     * @throws BleScanner.LocationPermissionNotGrantedException 未給予定位權限
     * @throws BleScanner.LocationDisabledException 未開啟定位
     */
    fun scan(button: View) {
        try {
            bleConnector.disconnect()
            bleScanner.startScan()

            button.isEnabled = false
            progressScanning.visibility = View.VISIBLE
            stopScanningCounter = 15

            bleList.clear()
            bleListDialogAdapter = object : ArrayAdapter<BluetoothDevice>(
                this,
                android.R.layout.simple_list_item_1,
                bleList
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)

                    val device = getItem(position)!!

                    (view as TextView).text = String.format("%s\n%s", device.name, device.address)

                    return view
                }
            }
            val dialog = AlertDialog.Builder(this)
                .setAdapter(bleListDialogAdapter) { dialog, index ->
                    bleListDialogAdapter?.getItem(index)?.run {
                        dialog.dismiss()
                        bleConnector.selectDevice(this)
                        bleConnector.connect()
                    }
                }.show()

            timer(null, true, 0, 1000) {
                if (bleConnector.selectedDevice() != null) {
                    cancel()

                    runOnUiThread {
                        bleScanner.stopScan()
                        button.isEnabled = true
                        progressScanning.visibility = View.GONE
                    }
                } else if (--stopScanningCounter <= 0) {
                    cancel()

                    runOnUiThread {
                        bleScanner.stopScan()
                        button.isEnabled = true
                        progressScanning.visibility = View.GONE
                        if (bleList.size == 0) {
                            dialog.dismiss()
                            Toast.makeText(this@MainActivityRMS, "未發現裝置", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: BleScanner.BluetoothNotEnabledException) {
            Log.i(">>>", "try enable bluetooth")
            bleScanner.enableBluetooth { enabled ->
                Log.i(">>>", "bluetooth enabled? $enabled")
                if (enabled) scan(button)
            }
        } catch (e: BleScanner.LocationPermissionNotGrantedException) {
            Log.i(">>>", "try to ask location permission")
            bleScanner.requestLocationPermission(this) { permissionGranted ->
                Log.i(">>>", "permission granted? $permissionGranted")
                if (permissionGranted) scan(button)
            }
        } catch (e: BleScanner.LocationDisabledException) {
            Log.i(">>>", "try to ask location on")
            Toast.makeText(this, "請開啟 GPS 定位以掃描附近裝置", Toast.LENGTH_SHORT).show()
            bleScanner.startLocationSettings(this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(Menu.NONE, 1, Menu.NONE, Pref_Threshold_Start.toUpperCase(Locale.getDefault()))
        menu?.add(Menu.NONE, 2, Menu.NONE, Pref_Threshold_End.toUpperCase(Locale.getDefault()))
        menu?.add(Menu.NONE, 3, Menu.NONE, Pref_Threshold_Range.toUpperCase(Locale.getDefault()))
        menu?.add(Menu.NONE, 4, Menu.NONE, Pref_Duration.toUpperCase(Locale.getDefault()))
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            val threshold = getPreferences(Context.MODE_PRIVATE)
                .getFloat(Pref_Threshold_Start, 0.0446f)

            Dialog(this).apply {
                setContentView(R.layout.dialog_input)
                findViewById<TextView>(R.id.title).text = "設定閥值 (起始點)"
                findViewById<TextView>(R.id.edit).text = threshold.toString()
                findViewById<View>(R.id.buttonOK).setOnClickListener {

                    val im: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    im.hideSoftInputFromWindow(it.windowToken, 0)

                    val input = findViewById<TextView>(R.id.edit).text.toString()

                    try {
                        val newThreshold = input.toFloat()
                        getPreferences(Context.MODE_PRIVATE).edit()
                            .putFloat(Pref_Threshold_Start, newThreshold)
                            .apply()

                        currentPage?.onThresholdStartChanged(newThreshold)
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                    dismiss()
                }
                findViewById<View>(R.id.buttonCancel).setOnClickListener {
                    dismiss()
                }
            }.also {
                it.show()
            }
        } else if (item.itemId == 2){
            val threshold = getPreferences(Context.MODE_PRIVATE)
                .getFloat(Pref_Threshold_End, 0.0446f)

            Dialog(this).apply {
                setContentView(R.layout.dialog_input)
                findViewById<TextView>(R.id.title).text = "設定閥值 (結束點)"
                findViewById<TextView>(R.id.edit).text = threshold.toString()
                findViewById<View>(R.id.buttonOK).setOnClickListener {

                    val im: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    im.hideSoftInputFromWindow(it.windowToken, 0)

                    val input = findViewById<TextView>(R.id.edit).text.toString()

                    try {
                        val newThreshold = input.toFloat()
                        getPreferences(Context.MODE_PRIVATE).edit()
                            .putFloat(Pref_Threshold_End, newThreshold)
                            .apply()

                        currentPage?.onThresholdEndChanged(newThreshold)
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                    dismiss()
                }
                findViewById<View>(R.id.buttonCancel).setOnClickListener {
                    dismiss()
                }
            }.also {
                it.show()
            }
        }else if(item.itemId == 3){
            //getSharePreference Setting
            val threshold = getPreferences(Context.MODE_PRIVATE)
                .getFloat(Pref_Threshold_Range, 0.2f)
            // Dialog Setting
            Dialog(this).apply {
                setContentView(R.layout.dialog_input)
                findViewById<TextView>(R.id.title).text = "設定閥值 (雜訊抑制)"
                findViewById<TextView>(R.id.edit).text = threshold.toString()
                findViewById<View>(R.id.buttonOK).setOnClickListener {

                    val im: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    im.hideSoftInputFromWindow(it.windowToken, 0)

                    val input = findViewById<TextView>(R.id.edit).text.toString()

                    try {
                        val newThreshold = input.toFloat()
                        getPreferences(Context.MODE_PRIVATE).edit()
                            .putFloat(Pref_Threshold_Range, newThreshold)
                            .apply()

                        currentPage?.onThresholdRangeChanged(newThreshold)
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                    dismiss()
                }
                findViewById<View>(R.id.buttonCancel).setOnClickListener {
                    dismiss()
                }
            }.also {
                it.show()
            }
        }else if(item.itemId == 4){
            //getSharePreference Setting
            val duration = getPreferences(Context.MODE_PRIVATE)
                .getFloat(Pref_Duration, 50f)
            // Dialog Setting
            Dialog(this).apply {
                setContentView(R.layout.dialog_input)
                findViewById<TextView>(R.id.title).text = "設定間距"
                findViewById<TextView>(R.id.edit).text = duration.toString()
                findViewById<View>(R.id.buttonOK).setOnClickListener {

                    val im: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    im.hideSoftInputFromWindow(it.windowToken, 0)

                    val input = findViewById<TextView>(R.id.edit).text.toString()

                    try {
                        val newDuration = input.toFloat()
                        getPreferences(Context.MODE_PRIVATE).edit()
                            .putFloat(Pref_Duration, newDuration)
                            .apply()

                        currentPage?.onDurationChanged(newDuration)
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                    dismiss()
                }
                findViewById<View>(R.id.buttonCancel).setOnClickListener {
                    dismiss()
                }
            }.also {
                it.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (bleScanner.handleBluetoothEnableResult(requestCode, resultCode)) {
            return
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        bleScanner.handleRequestPermissionResult(requestCode, permissions, grantResults)
    }
}
