package com.jonataprezende.medreminder

import android.app.Application
import com.jonataprezende.medreminder.data.repository.MedRepository

class MedReminderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializa o repository singleton
        MedRepository.getInstance(this)
    }
}