package com.hizkia.homescreen.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Scope yang hidup selama proses, untuk tulisan DB yang tidak boleh batal ketika layar ditutup. */
object AppScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)
