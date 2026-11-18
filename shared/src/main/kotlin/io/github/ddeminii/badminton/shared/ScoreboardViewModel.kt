package io.github.ddeminii.badminton.shared

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import io.github.ddeminii.badminton.shared.cheer.CheerSchedule
import io.github.ddeminii.badminton.shared.gesture.GestureAction
import io.github.ddeminii.badminton.shared.haptics.HapticPattern
import io.github.ddeminii.badminton.shared.haptics.Haptics
import io.github.ddeminii.badminton.shared.model.GameRules
import io.github.ddeminii.badminton.shared.model.ScoreState
import io.github.ddeminii.badminton.shared.model.Side
import io.github.ddeminii.badminton.shared.sync.ScoreSync
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** 워치와 폰이 똑같이 쓰는 점수판 로직: 상태 보관, 진동, 동기화. */
class ScoreboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("score", Context.MODE_PRIVATE)
    private val haptics = Haptics(application)
    private val sync = ScoreSync(application, ::onRemoteState)

    /** 워치는 폰에서 바뀐 점수도 진동으로 알려준다. 세워 둔 폰은 직접 누를 때만 진동한다. */
    private val vibrateOnRemoteChange =
        application.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ScoreState> = _state.asStateFlow()

    private val cheerSchedule = CheerSchedule()
    private val _cheers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 응원을 내보낼 때가 되면 값이 하나 나온다. 어느 기기에서 점수를 올렸든 몇 점마다 한 번씩. */
    val cheers: SharedFlow<Unit> = _cheers.asSharedFlow()

    init {
        sync.start()
        // 지난번에 보내지 못한 변경이 있을 수 있으니 한 번 더 올려 둔다.
        if (_state.value.timestamp > 0) sync.push(_state.value)
    }

    fun onAction(action: GestureAction) {
        val old = _state.value
        val now = System.currentTimeMillis()
        val (new, pattern) = when (action) {
            is GestureAction.Increment -> old.increment(action.side, now) to
                if (action.side == Side.ME) HapticPattern.MY_POINT else HapticPattern.OPPONENT_POINT
            is GestureAction.Decrement -> old.decrement(action.side, now) to HapticPattern.MINUS
            GestureAction.Reset -> old.reset(now) to HapticPattern.NEW_GAME
        }
        if (new == old) return // 0점에서 −1: 아무 일도 일어나지 않음

        commit(new)
        haptics.play(if (justWon(old, new)) HapticPattern.WIN else pattern)
        sync.push(new)
        if (action is GestureAction.Increment) countPointForCheer()
    }

    private fun onRemoteState(remote: ScoreState) {
        val old = _state.value
        if (!remote.isNewerThan(old)) return

        commit(remote)
        val pattern = HapticPattern.forChange(old, remote)
        if (vibrateOnRemoteChange) {
            (if (justWon(old, remote)) HapticPattern.WIN else pattern)?.let(haptics::play)
        }
        if (pattern == HapticPattern.MY_POINT || pattern == HapticPattern.OPPONENT_POINT) countPointForCheer()
    }

    private fun justWon(old: ScoreState, new: ScoreState) =
        GameRules.winner(old) == null && GameRules.winner(new) != null

    private fun countPointForCheer() {
        if (cheerSchedule.onPoint()) _cheers.tryEmit(Unit)
    }

    private fun commit(state: ScoreState) {
        _state.value = state
        prefs.edit {
            putInt(KEY_ME, state.me)
            putInt(KEY_OPPONENT, state.opponent)
            putLong(KEY_TIMESTAMP, state.timestamp)
        }
    }

    private fun load() = ScoreState(
        me = prefs.getInt(KEY_ME, 0),
        opponent = prefs.getInt(KEY_OPPONENT, 0),
        timestamp = prefs.getLong(KEY_TIMESTAMP, 0L),
    )

    override fun onCleared() {
        sync.stop()
    }

    private companion object {
        const val KEY_ME = "me"
        const val KEY_OPPONENT = "opponent"
        const val KEY_TIMESTAMP = "timestamp"
    }
}
