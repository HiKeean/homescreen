package com.hizkia.homescreen.policy

data class UsageEv(val pkg: String, val resumed: Boolean, val ts: Long)

object UsageMath {
    /**
     * Total milidetik tiap package berada di depan. Satu app bisa punya beberapa activity yang
     * resume/pause bergantian, jadi dihitung lewat kedalaman: waktu jalan selama ada minimal satu activity aktif.
     * Activity yang masih aktif di akhir dihitung sampai [nowMs].
     */
    fun foregroundMs(events: List<UsageEv>, nowMs: Long): Map<String, Long> {
        val depth = HashMap<String, Int>()
        val since = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        for (e in events.sortedBy { it.ts }) {
            val d = depth[e.pkg] ?: 0
            if (e.resumed) {
                if (d == 0) since[e.pkg] = e.ts
                depth[e.pkg] = d + 1
            } else if (d > 0) {
                if (d == 1) total[e.pkg] = (total[e.pkg] ?: 0L) + (e.ts - since.getValue(e.pkg))
                depth[e.pkg] = d - 1
            }
        }
        for ((pkg, d) in depth) {
            if (d > 0) total[pkg] = (total[pkg] ?: 0L) + (nowMs - since.getValue(pkg))
        }
        return total
    }
}
