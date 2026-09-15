package com.zabtaai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    private lateinit var statusIndicator: TextView
    private lateinit var statusText: TextView
    private lateinit var currentCommandText: TextView
    private lateinit var activityLog: LinearLayout
    private lateinit var btnActivate: Button
    private lateinit var btnDeactivate: Button
    
    private var isActive = false
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.POST_NOTIFICATIONS
    )
    private val PERMISSION_REQUEST_CODE = 100
    
    // Core components
    private var speechEngine: SpeechRecognitionEngine? = null
    private var screenObserver: ScreenObserver? = null
    private var actionExecutor: ActionExecutor? = null
    private var planner: Planner? = null
    private var stateManager: StateManager? = null
    private var orchestrator: OperatorOrchestrator? = null
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupListeners()
        requestRequiredPermissions()
        initializeComponents()
        checkAccessibilityServiceStatus()
    }

    private fun initializeViews() {
        statusIndicator = findViewById(R.id.status_indicator)
        statusText = findViewById(R.id.status_text)
        currentCommandText = findViewById(R.id.current_command)
        activityLog = findViewById(R.id.activity_log)
        btnActivate = findViewById(R.id.btn_activate)
        btnDeactivate = findViewById(R.id.btn_deactivate)
    }

    private fun setupListeners() {
        btnActivate.setOnClickListener {
            activateZabtaAI()
        }
        
        btnDeactivate.setOnClickListener {
            deactivateZabtaAI()
        }
    }

    private fun initializeComponents() {
        try {
            // Initialize state manager
            stateManager = StateManager(this)
            addToLog("✓ State Manager initialized")
            
            // Initialize planner
            planner = Planner()
            addToLog("✓ Planner initialized")
            
            // Note: Other components require AccessibilityService which runs separately
            addToLog("✓ Core components ready")
        } catch (e: Exception) {
            addToLog("✗ Initialization error: ${e.message}")
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission)
            }
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun activateZabtaAI() {
        isActive = true
        stateManager?.setActive(true)
        statusIndicator.text = "ACTIVE"
        statusIndicator.setTextColor(resources.getColor(R.color.success, null))
        statusText.text = "Status: ZabtaAI Active - Listening for commands"
        
        // Start foreground service
        val serviceIntent = Intent(this, ZabtaAIForegroundService::class.java)
        startService(serviceIntent)
        
        addToLog("🟢 ZabtaAI Activated")
    }

    private fun deactivateZabtaAI() {
        isActive = false
        stateManager?.setActive(false)
        statusIndicator.text = "OFF"
        statusIndicator.setTextColor(resources.getColor(R.color.error, null))
        statusText.text = "Status: ZabtaAI Deactivated"
        
        // Stop foreground service
        val serviceIntent = Intent(this, ZabtaAIForegroundService::class.java)
        stopService(serviceIntent)
        
        addToLog("🔴 ZabtaAI Deactivated")
    }

    private fun checkAccessibilityServiceStatus() {
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()
        if (isAccessibilityEnabled) {
            addToLog("✓ Accessibility Service Enabled")
        } else {
            addToLog("⚠ Accessibility Service Disabled - Enable in Settings")
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        return accessibilityManager.isEnabled
    }

    private fun addToLog(message: String) {
        val logEntry = TextView(this).apply {
            text = message
            textSize = 12f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            setPadding(8, 4, 8, 4)
        }
        activityLog.addView(logEntry, 0)
        
        // Keep only last 20 entries
        while (activityLog.childCount > 20) {
            activityLog.removeViewAt(activityLog.childCount - 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            
            if (allGranted) {
                addToLog("✓ All permissions granted")
            } else {
                addToLog("⚠ Some permissions denied")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        speechEngine?.release()
        screenObserver?.release()
    }
}
