package com.conadasoft.rosariodiario

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.conadasoft.rosariodiario.databinding.ActivityAppsBinding

class AppsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        eventos()
    }

    private fun eventos() {
        binding.logoAmor.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.callecaboapp.callecabo.amordiario&hl=es_419")
            )
            startActivity(intent)
        }

        binding.tituloAmor.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.callecaboapp.callecabo.amordiario&hl=es_419")
            )
            startActivity(intent)
        }

        binding.logoDiario.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.conadasoft.simplediario")
            )
            startActivity(intent)
        }

        binding.tituloDiario.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.conadasoft.simplediario")
            )
            startActivity(intent)
        }

        binding.logoRosario.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.conadasoft.chistediario&hl=es")
            )
            startActivity(intent)
        }

        binding.tituloRosario.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.conadasoft.rosariodiario&hl=es")
            )
            startActivity(intent)
        }

        binding.logoColors.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.conada.colors")
            )
            startActivity(intent)
        }

        binding.tituloColors.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.conada.colors")
            )
            startActivity(intent)
        }

        binding.logoPepeJones.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.conadasoft.PepeJones&hl=es_419")
            )
            startActivity(intent)
        }

        binding.tituloPepeJones.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.conadasoft.PepeJones&hl=es_419")
            )
            startActivity(intent)
        }

        binding.logoIMC.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.conadasoft.imccalculadorapesoideal")
            )
            startActivity(intent)
        }

        binding.tituloIMC.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.conadasoft.imccalculadorapesoideal")
            )
            startActivity(intent)
        }
    }
}