package com.hizkia.homescreen.data

import android.content.Context
import android.content.SharedPreferences
import com.hizkia.homescreen.policy.RuleChange
import com.hizkia.homescreen.policy.RulesState
import com.hizkia.homescreen.policy.SosmedRules

/** Menyimpan aturan sosmed (yang berlaku + perubahan tertunda) di SharedPreferences. */
object SosmedStore {
    private const val FILE = "sosmed"

    fun get(context: Context, nowMs: Long = System.currentTimeMillis()): RulesState {
        val prefs = prefs(context)
        val stored = read(prefs)
        val settled = RuleChange.settle(stored, nowMs)
        if (settled != stored) write(prefs, settled)
        return settled
    }

    fun propose(context: Context, proposed: SosmedRules, nowMs: Long = System.currentTimeMillis()): RulesState {
        val next = RuleChange.propose(get(context, nowMs), proposed, nowMs)
        write(prefs(context), next)
        return next
    }

    private fun read(p: SharedPreferences): RulesState {
        val effective = SosmedRules(
            packages = p.getStringSet("pkgs", emptySet())?.toSet() ?: emptySet(),
            limitMin = p.getInt("limit", SosmedRules.DEFAULT.limitMin),
            activeDays = p.getString("days", null)?.toDays() ?: SosmedRules.DEFAULT.activeDays,
        )
        val pendingAt = p.getLong("p_at", 0L)
        val pending = if (pendingAt > 0) {
            SosmedRules(
                packages = p.getStringSet("p_pkgs", emptySet())?.toSet() ?: emptySet(),
                limitMin = p.getInt("p_limit", effective.limitMin),
                activeDays = p.getString("p_days", null)?.toDays() ?: effective.activeDays,
            )
        } else null
        return RulesState(effective, pending, pendingAt)
    }

    private fun write(p: SharedPreferences, s: RulesState) {
        p.edit().apply {
            putStringSet("pkgs", s.effective.packages)
            putInt("limit", s.effective.limitMin)
            putString("days", s.effective.activeDays.joinToString(""))
            if (s.pending != null) {
                putStringSet("p_pkgs", s.pending.packages)
                putInt("p_limit", s.pending.limitMin)
                putString("p_days", s.pending.activeDays.joinToString(""))
                putLong("p_at", s.pendingAtMs)
            } else {
                remove("p_pkgs"); remove("p_limit"); remove("p_days"); remove("p_at")
            }
        }.apply()
    }

    private fun String.toDays(): Set<Int> = mapNotNull { it.digitToIntOrNull() }.toSet()

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
