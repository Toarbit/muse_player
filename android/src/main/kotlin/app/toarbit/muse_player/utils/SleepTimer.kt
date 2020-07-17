package app.toarbit.muse_player.utils

import android.os.CountDownTimer

/**
 * Created by omit on 2020/6/26 for android.
 */

class SleepTimer(millisInFuture: Long, countDownInterval: Long) : CountDownTimer(millisInFuture, countDownInterval) {

    override fun onFinish() {
        callbacks.forEach {
            it.onFinish()
        }
    }

    override fun onTick(millisUntilFinished: Long) {
        millisUntilFinish = millisUntilFinished
    }

    interface Callback {
        fun onFinish()
    }

    companion object {
        /** 定时关闭剩余时间 */
        @JvmStatic
        private var millisUntilFinish: Long = 0L

        @JvmStatic
        fun getMillisUntilFinish(): Long {
            return millisUntilFinish
        }

        @JvmStatic
        private var instance: SleepTimer? = null

        private val callbacks: MutableList<Callback> by lazy { ArrayList<Callback>() }

        @JvmStatic
        fun isTicking(): Boolean {
            return millisUntilFinish > 0L
        }

        /**
         * 开始或者停止计时
         * @param start
         * @param duration
         */
        @JvmStatic
        fun toggleTimer(duration: Long): Boolean {
//            val context = App.getContext()
            if (duration <= 0L || instance != null) {
                instance?.cancel()
                instance = null
                val ticking = if (duration <= 0L) isTicking() else false
                millisUntilFinish = 0L

                return ticking
            }
            instance = SleepTimer(duration, 1000).apply { start() }
            return true
//            ToastUtil.show(context, if (!start) context.getString(R.string.cancel_timer) else context.getString(R.string.will_stop_at_x, Math.ceil((duration / 1000 / 60).toDouble()).toInt()))
        }

        public fun addCallback(callback: Callback) {
            callbacks.add(callback)
        }

    }
}