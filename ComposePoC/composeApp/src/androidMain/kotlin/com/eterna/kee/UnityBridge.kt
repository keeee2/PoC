package com.eterna.kee

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.unity3d.player.IUnityPlayerLifecycleEvents
import com.unity3d.player.UnityPlayer
import com.unity3d.player.UnityPlayerForActivityOrService
import java.lang.ref.WeakReference

@Suppress("StaticFieldLeak")
object UnityBridge {
    private var unityPlayer: UnityPlayerForActivityOrService? = null
    private var  cachedViewRef: WeakReference<View>? = null

    // Unity → Compose 콜백
    var onMessageFromUnity: ((gameObject: String, method: String, message: String) -> Unit)? = null

    fun init(activity: Activity) {
        if (unityPlayer == null) {
            unityPlayer = UnityPlayerForActivityOrService(activity, object : IUnityPlayerLifecycleEvents {
                override fun onUnityPlayerUnloaded() {}
                override fun onUnityPlayerQuitted() {}
            })
        }
    }

    fun getView(): View? {
        cachedViewRef?.get()?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            return view
        }

        val view = unityPlayer?.getView() ?: return null
        (view.parent as? ViewGroup)?.removeView(view)
        unityPlayer?.windowFocusChanged(true)
        unityPlayer?.resume()
        cachedViewRef = WeakReference(view)
        return view
    }

    // Compose → Unity
    fun sendMessage(gameObject: String, method: String, message: String) {
        UnityPlayer.UnitySendMessage(gameObject, method, message)
    }

    // Unity C#에서 호출: AndroidJavaClass("com.eterna.kee.UnityBridge").CallStatic("receiveFromUnity", "obj", "method", "msg")
    @JvmStatic
    fun receiveFromUnity(gameObject: String, method: String, message: String) {
        onMessageFromUnity?.invoke(gameObject, method, message)
    }

    fun resume() { unityPlayer?.resume() }
    fun pause() { unityPlayer?.pause() }
    fun destroy() {
        unityPlayer?.destroy()
        unityPlayer = null
        cachedViewRef = null
    }
}