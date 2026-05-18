package com.estimaph.app

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.estimaph.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        animateEntrance()
        setupListeners()
    }

    // Staggered entrance: brand slides down, form slides up, sign-up link fades in last
    private fun animateEntrance() {
        val interp = DecelerateInterpolator(1.5f)

        b.llBrand.translationY = -30f
        b.llBrand.animate().alpha(1f).translationY(0f)
            .setDuration(500).setStartDelay(100).setInterpolator(interp).start()

        b.cardForm.translationY = 50f
        b.cardForm.animate().alpha(1f).translationY(0f)
            .setDuration(500).setStartDelay(220).setInterpolator(interp).start()

        b.tvSignup.animate().alpha(1f)
            .setDuration(400).setStartDelay(500).setInterpolator(interp).start()
    }

    private fun setupListeners() {
        b.etPassword.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) { login(); true } else false
        }
        b.btnLogin.setOnClickListener { login() }
        b.tvForgot.setOnClickListener {
            Toast.makeText(this, "Password reset coming soon.", Toast.LENGTH_SHORT).show()
        }
        b.tvSignup.setOnClickListener {
            Toast.makeText(this, "Sign-up coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun login() {
        val email = b.etEmail.text.toString().trim()
        val pass  = b.etPassword.text.toString()

        b.tilEmail.error    = null
        b.tilPassword.error = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilEmail.error = "Enter a valid email address"
            b.tilEmail.requestFocus()
            return
        }
        if (pass.length < 6) {
            b.tilPassword.error = "Password must be at least 6 characters"
            b.tilPassword.requestFocus()
            return
        }

        // TODO: replace with real auth call
        // authRepo.login(email, pass, onSuccess = ::navigateToMain, onError = ::showError)

        b.btnLogin.isEnabled = false
        b.btnLogin.text      = "Signing in…"

        b.root.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 700)
    }
}
