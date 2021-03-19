package com.narratiive.narratiivesdk


import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
    private var isDebug: Boolean = false


    fun init(context: Context, isDebug: Boolean = false) {
        this.isDebug = isDebug
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

        this.log("Init completed")
    }

    fun send(screenName: String?) {
        this.log("Send started")
        this.createHit(screenName)
        this.log("Send completed")
    }

    private fun log(msg: String) {
        if (this.isDebug) {
            Log.d("NarratiiveSDK", msg)
        }
    }

    private fun loadAdvertisingId(cb: (String?) -> Unit) {
        this.log("Loading AAID")
        Thread(Runnable {
            try {
                val info = AdvertisingIdClient.getAdvertisingIdInfo(this.context)
                cb(info.id)
                this.log("AAID loaded: ${info.id}")
            } catch (e: Exception) {
                cb(null)
                this.log("AAID NOT loaded.")
            }
        }).start()
    }


    private fun createToken(advertisingId: String? = null) {
        this.log("Creating token with AAID: $advertisingId")
        val tokenInfo = TokenInfo(host = this.host, hostKey = this.hostKey, aaid = advertisingId)

        apiService.createToken(tokenInfo) {
            this.token = it?.token
            this.log("Token created: ${it?.token}")
        }
    }

    private fun createHit(path: String?) {
        if (this.token != null && !isSending) {
            this.log("Creating hit with path: $path")
            val hitInfo =
                HitInfo(host = this.host, hostKey = this.hostKey, token = this.token!!, path = path)
            isSending = true
            apiService.createHit(hitInfo) {
                this.token = it?.token
                saveToken()
                isSending = false
                this.log("Hit created with new token: ${it?.token}")
            }
        }
    }

    private fun saveToken() {
        this.token?.let {
            this.log("Saving token: $it")
            with(this.sharedPref.edit()) {
                putString("token", it)
                apply()
            }
        }
    }
}