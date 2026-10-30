package dev.haxzie.relaybird.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * Advertises the bridge over mDNS. Only registration is used — the Mac does the browsing — so
 * none of the deprecated resolveService surface is involved.
 */
class NsdAdvertiser(context: Context) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var listener: NsdManager.RegistrationListener? = null

    fun register(port: Int, deviceId: String, deviceName: String) {
        unregister()
        val info = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("v", "1")
            setAttribute("did", deviceId)
            setAttribute("name", deviceName)
        }
        // NsdManager rejects a reused listener with "listener already in use", so each
        // registration gets a fresh one.
        val registration = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.i(TAG, "registered as ${info.serviceName}")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "registration failed: $errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
        }
        listener = registration
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, registration)
    }

    fun unregister() {
        listener?.let { runCatching { nsd.unregisterService(it) } }
        listener = null
    }

    companion object {
        private const val TAG = "NsdAdvertiser"
        const val SERVICE_TYPE = "_relaybird._tcp"
        const val SERVICE_NAME = "RelayBird"
    }
}
