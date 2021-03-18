package com.narratiive.narratiivesdk


import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.ads.identifier.AdvertisingIdClient

object NarratiiveSDK {
    private const val SHARED_PREF_FILE = "com.narratiive.sdk.sharepref"

    private lateinit var context: Context
    private lateinit var host: String
    private lateinit var hostKey: String
    private lateinit var sharedPref: SharedPreferences
    private lateinit var apiService: NarratiiveApiService

    private var useAaid: Boolean = false
    private var token: String? = null
    private var isSending: Boolean = false


    fun init(context: Context) {
        this.context = context
        this.apiService = NarratiiveApiService(context)
        this.host = context.getString(R.string.narratiive_host)
        this.hostKey = context.getString(R.string.narratiive_hostkey)
        this.useAaid = context.getString(R.string.narratiive_use_aaid) == "1"
        this.sharedPref = context.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE)
        this.token = this.sharedPref.getString("token", null)

        if (this.token == null) {
            if (this.useAaid) {
                loadAdvertisingId(NarratiiveSDK::createToken)
            } else {
                createToken()
            }
        }
    }

    fun send(screenName: String?) {
        this.createHit(screenName)
    }

    private fun loadAdvertisingId(cb: (String?) -> Unit) {
        Thread(Runnable {
            try {
                val info = AdvertisingIdClient.getAdvertisingIdInfo(this.context)
                cb(info.id)
            } catch (e: Exception) {
                cb(null)
            }
        }).start()
    }


    private fun createToken(advertisingId: String? = null) {
        val tokenInfo = TokenInfo(host = this.host, hostKey = this.hostKey, aaid = advertisingId)

        apiService.createToken(tokenInfo) {
            this.token = it?.token
        }
    }

    private fun createHit(path: String?) {
        if (this.token != null && !isSending) {
            val hitInfo =
                HitInfo(host = this.host, hostKey = this.hostKey, token = this.token!!, path = path)
            isSending = true
            apiService.createHit(hitInfo) {
                this.token = it?.token
                saveToken()
                isSending = false
            }
        }
    }

    private fun saveToken() {
        this.token?.let {
            with(this.sharedPref.edit()) {
                putString("token", it)
                apply()
            }
        }
    }
}