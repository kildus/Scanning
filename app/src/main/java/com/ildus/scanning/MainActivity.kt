package com.ildus.scanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ildus.scanning.databinding.ActivityMainBinding
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.*

const val TAG = "myTag"
private const val IntentFilterAction = "hsm.RECVRBI"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val tickReceiver by lazy { makeBroadcastReceiver() }
    private val packViewModel by lazy { ViewModelProvider(this).get(PackViewModel::class.java) }

    var myLiveData: MutableLiveData<Pack> = MutableLiveData()

    private val client = OkHttpClient()

    val URL = "https://api.icndb.com/jokes/random"
    var okHttpClient: OkHttpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        Log.d(TAG, "onCreate")

        lifecycle.addObserver(MyLifecycle())

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

//        if (savedInstanceState != null) {
//            savedInstanceState.getSerializable("packs")?.let {
//                packs = it as MutableList<Pack>
//            }
//        }

        if (packViewModel.packs.isNullOrEmpty()) fillPacks()

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = PackRecyclerAdapter(packViewModel.packs)

        showMainInfo()

//        packViewModel.myData.value = "ildus"
        packViewModel.myData.observe(this, Observer {
            it?.let {
                Log.d(TAG, "observe")
            }
        })

//        myLiveData = DataController().myLiveData

        myLiveData.observe(this, Observer {
            it?.let {
                Log.d(TAG, "DataController observe")
            }
        })

        myLiveData.value = Pack()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

//        outState.putSerializable("packs", packs as Serializable)
    }

    private fun fillPacks() {
        for (i in 1..50) {
            packViewModel.packs.add(Pack("barcode$i", false, false))
        }
    }

    override fun onStart() {
        super.onStart()

        Log.d(TAG, "onStart")

        binding.editTextView.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    handleEditText()
//                    Log.d(TAG, "IME_ACTION_SEND")
                    true
                }
                else -> false
            }
        }

        binding.putTextButton.setOnClickListener {
            handleEditText()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    fun handleEditText() {
        if (binding.editTextView.text.isNotEmpty()) {

            val text = binding.editTextView.text.toString().trim()
            binding.editTextView.setText("")

            for ((index, pack) in packViewModel.packs.withIndex()) {
                if (pack.code == text) {

                    if (pack.isSurplus || pack.isFound) {
                        Toast.makeText(this, getString(R.string.already_added), Toast.LENGTH_SHORT)
                            .show()
                        showMainInfo()
                        return
                    }

                    pack.isFound = true
//                        recyclerView.adapter?.notifyItemChanged(packs.size - 1)
//                    recyclerView.adapter?.notifyDataSetChanged()

                    packViewModel.packs.removeAt(index)
                    binding.recyclerView.adapter?.notifyItemRemoved(index)

                    packViewModel.packs.add(0, pack)
                    binding.recyclerView.adapter?.notifyItemInserted(0)

//                    recyclerView.adapter?.notifyItemMoved(index, 0)
//                    recyclerView.adapter?.notifyItemInserted(0)

                    binding.recyclerView.smoothScrollToPosition(0)

//                    packViewModel.packs.add(0, pack)
//            recyclerView.adapter?.notifyItemChanged(packViewModel.packs.size - 1)
//                    recyclerView.adapter?.notifyItemInserted(0)

                    Toast.makeText(this, getString(R.string.is_found), Toast.LENGTH_SHORT).show()
                    showMainInfo()
                    return
                }
            }

//            packViewModel.packs.add(Pack(text, false, true))
            packViewModel.packs.add(0, Pack(text, false, true))
//            recyclerView.adapter?.notifyItemChanged(packViewModel.packs.size - 1)
            binding.recyclerView.adapter?.notifyItemInserted(0)
            Toast.makeText(this, getString(R.string.is_surplus), Toast.LENGTH_SHORT).show()

            binding.recyclerView.smoothScrollToPosition(0)

            showMainInfo()
        }
    }

    fun showMainInfo() {
        var found = 0
        var notFound = 0
        var surplus = 0

        for (pack in packViewModel.packs) {
            if (pack.isSurplus) surplus++ else if (pack.isFound) found++ else notFound++
        }

        binding.textViewFound.text = found.toString()
        binding.textViewNotFound.text = notFound.toString()
        binding.textViewSurplus.text = surplus.toString()

    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        registerReceiver(tickReceiver, IntentFilter(IntentFilterAction))
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        try {
            unregisterReceiver(tickReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Time tick Receiver not registered", e)
        }
    }

    private fun makeBroadcastReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                // https://support.honeywellaidc.com/s/article/Scanning-using-a-Broadcast-Data-Intent-on-CT50-and-CN51
                Log.d(TAG, "onReceive")
//                Toast.makeText(this@MainActivity, "onReceive", Toast.LENGTH_SHORT).show()
//                if (intent?.action == Intent.ACTION_TIME_TICK) {
//                    dateTimeTextView.text = getCurrentTimeStamp()
//                    Log.d(TAG, "" + dateTimeTextView.text)
//                }

//                Log.d(TAG, "codeId: \t" + intent?.getStringExtra("codeId"))
//                Log.d(TAG, "dataBytes\t" + Arrays.toString(intent?.getByteArrayExtra("dataBytes")))
                val d = intent?.getStringExtra("data")
//                Log.d(TAG, "data: \t" + intent?.getStringExtra("data"))
//                Log.d(TAG, "timestamp: \t" + intent?.getStringExtra("timestamp"))
//                Log.d(TAG, "aimId: \t" + intent?.getStringExtra("aimId"))
//                Log.d(TAG, "version: \t" + intent?.getIntExtra("version", -1))
//                Log.d(TAG, "charset: \t" + intent?.getStringExtra("charset"))
//                Log.d(TAG, "scanner: \t" + intent?.getStringExtra("scanner"))
                if (d != null && d.isNotEmpty()) {
//                    textView.text = d
                    val text = cleanTextContent(d)
                    Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
                    binding.editTextView.setText(text)
                    handleEditText()
                }

            }
        }
    }

    fun cleanTextContent(str: String): String {
        var text = str
        text = text.replace("[^\\x00-\\x7F]".toRegex(), "")

        // erases all the ASCII control characters
        text = text.replace("[\\p{Cntrl}&&[^\r\n\t]]".toRegex(), "")

        // removes non-printable characters from Unicode
        text = text.replace("\\p{C}".toRegex(), "")
        return text.trim()
    }

    fun runTest() {
        val request = Request.Builder()
            .url("https://publicobject.com/helloworld.txt")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            for ((name, value) in response.headers) {
                println("$name: $value")
            }

            println(response.body!!.string())
        }
    }

    private fun loadRandomFact() {
        runOnUiThread {
//            progressBar.visibility = View.VISIBLE
        }

        val request: Request = Request.Builder().url(URL).build()
        okHttpClient.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Failed to fetch photos", e)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                val json = response.body?.string()

                Log.d(TAG, "body: $json")

                val txt = (JSONObject(json).getJSONObject("value").get("joke")).toString()


                runOnUiThread {
                    Toast.makeText(this@MainActivity, "" + txt, Toast.LENGTH_SHORT).show()
                }

                Log.d(TAG, "onResponse: $txt")
            }
        })

    }

    fun onClickNotFound(view: View) {

//        runTest()

        loadRandomFact()

//        liveData?.setValue("new value")
        packViewModel.myData.value = "ild"

//        myLiveData.value?.isFound = true
        myLiveData.value = myLiveData.value?.apply {
            isFound = true
        }
    }

}
