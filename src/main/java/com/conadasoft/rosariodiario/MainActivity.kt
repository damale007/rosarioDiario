package com.conadasoft.rosariodiario

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.conadasoft.rosariodiario.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var voyMisterio = 0
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var misterio1: String
    private var misterio2: String? = null
    private var misterio3: String? = null
    private var misterio4: String? = null
    private var misterio5: String? = null
    private var meditacionMisterio1: String? = null
    private var meditacionMisterio2: String? = null
    private var meditacionMisterio3: String? = null
    private var meditacionMisterio4: String? = null
    private var meditacionMisterio5: String? = null
    private var tituloMisterios: String? = null
    private var tipoMisterio: Int = 0
    private var voz: Boolean = false
    private var avemaria = 0
    private var meditados = false
    private var lento = 3
    private var estaHablando = false
    private val adRequest = AdRequest.Builder().build()
    private var mInterstitialAd: InterstitialAd? = null
    private var salir = false
    private var ajustes = false
    private var ultimoAnuncio: Long = 0

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // TextToSpeech inicializado correctamente
                val result = textToSpeech.setLanguage(Locale.getDefault()) // Configura el idioma

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Idioma no disponible para leer", Toast.LENGTH_SHORT).show()
                }

                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        // Se llama cuando comienza la síntesis de voz de una frase
                        if (utteranceId == "1") {
                            avemaria++
                            runOnUiThread {
                                binding.numeroAveMaria.text = avemaria.toString()
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {// Se llama cuando termina la síntesis de voz de una frase
                        if (utteranceId == "2") {
                            avemaria++
                            runOnUiThread {
                                binding.numeroAveMaria.text = avemaria.toString()
                            }
                        } else if (utteranceId == "9") {
                            muestraInterstitial()
                        }
                    }

                    override fun onError(utteranceId: String?) {
                    }
                })
            }
        }

        val pref = PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )
        meditados = pref.getBoolean("meditados", false)
        lento = pref.getInt("velocidad", 3)
        val notificaciones = pref.getBoolean("notificacion", true)

        if (notificaciones)
            alarma()
        else
            borraAlarma()

        permisos()

        voyMisterio = 0
        misterio1 = ""
        misterio2 = ""
        misterio3 = ""
        misterio4 = ""
        misterio5 = ""
        meditacionMisterio1 = ""
        meditacionMisterio2 = ""
        meditacionMisterio3 = ""
        meditacionMisterio4 = ""
        meditacionMisterio5 = ""
        tituloMisterios = ""
        tipoMisterio = 0
        avemaria = 0
        voz = false

        binding.adView.loadAd(adRequest)

        if (voyMisterio == 0) binding.contadorAveMarias.visibility = View.GONE

        asignaMisterios()
        rellenaContenido(0)
        publicidad()

        eventos()
    }

    private fun eventos() {
        binding.settings.setOnClickListener {
            binding.leeme.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_action_leer))
            textToSpeech.stop()
            voz = false

            ajustes = true;
            val intent = Intent(this, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        binding.botonSiguienteMisterio.setOnClickListener {
            voyMisterio++
            if (voyMisterio == 6) finish()
            if (voyMisterio == 5) {
                binding.botonSiguienteMisterio.text = getString(R.string.botonIniciar)
            } else binding.botonSiguienteMisterio.text = getString(R.string.botonSiguiente)

            if (estaHablando) {
                textToSpeech.stop()
            }

            avemaria = 0
            binding.botonSiguienteAveMaria.isEnabled = voyMisterio > 0

            ponAveMaria(avemaria)
            rellenaContenido(voyMisterio)
            binding.contadorAveMarias.visibility = View.VISIBLE

            val misterio = resources.getStringArray(R.array.misterios)

            binding.misterioNumero.text = misterio[voyMisterio]
            binding.adView.loadAd(adRequest)

            if (voz) activaSonido()

            publicidad()
        }

        binding.botonSiguienteAveMaria.setOnClickListener {
            avemaria++
            ponAveMaria(avemaria)
            if (avemaria == 10) binding.botonSiguienteAveMaria.isEnabled = false
        }

        binding.leeme.setOnClickListener {
            if (estaHablando) {
                binding.leeme.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_action_leer))
                textToSpeech.stop()
                voz = false
            } else {
                binding.leeme.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_action_stop))
                voz = true
                activaSonido()
            }
            estaHablando = !estaHablando
        }
    }

    private fun muestraInterstitial() {
        // Muestra elanuncio intersticial
        val ahora = LocalDateTime.now()
        val formato = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

        if (ahora.format(formato).toLong() > ultimoAnuncio +4) {
            ultimoAnuncio = ahora.format(formato).toLong()

            mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // El anuncio se cerró
                    if (voz) activaSonido()

                    // Aquí puedes realizar las acciones que deseas después de que el anuncio se cierre
                    // Por ejemplo, continuar con el flujo de la app, cargar un nuevo nivel, etc.
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Error al mostrar el anuncio
                    if (voz) activaSonido()
                }

                override fun onAdShowedFullScreenContent() {
                    // El anuncio se mostró
                    mInterstitialAd = null
                    if (salir) finish()
                }
            }

            mInterstitialAd!!.show(this)
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val pref = PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )

        meditados = pref.getBoolean("meditados", false)
        lento = pref.getInt("velocidad", 3)
        val notificaciones = pref.getBoolean("notificacion", true)

        if (notificaciones)
            alarma()
        else {
            borraAlarma()
        }

        asignaMisterios()
        rellenaContenido(voyMisterio)

        if (ajustes) {
            ajustes = false
            muestraInterstitial()
            publicidad()
        }
    }

    private fun publicidad() {
        mInterstitialAd = null
        InterstitialAd.load(
            this, "ca-app-pub-8408332664043957/2926625867", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d("MainActivity", loadAdError.toString())
                    mInterstitialAd = null
                }
            })
    }

    private fun asignaMisterios() {
        val now = Calendar.getInstance()
        val dia = now[Calendar.DAY_OF_WEEK] - 1

        if (dia == 0 || dia == 3) {
            tituloMisterios = resources.getString(R.string.misterioGlorioso)
            tipoMisterio = 3
            misterio1 = resources.getString(R.string.glorioso1)
            meditacionMisterio1 = resources.getString(R.string.meditacionGlorioso1)

            misterio2 = resources.getString(R.string.glorioso2)
            meditacionMisterio2 = resources.getString(R.string.meditacionGlorioso2)

            misterio3 = resources.getString(R.string.glorioso3)
            meditacionMisterio3 = resources.getString(R.string.meditacionGlorioso3)

            misterio4 = resources.getString(R.string.glorioso4)
            meditacionMisterio4 = resources.getString(R.string.meditacionGlorioso4)

            misterio5 = resources.getString(R.string.glorioso5)
            meditacionMisterio5 = resources.getString(R.string.meditacionGlorioso5)
        }

        if (dia == 1 || dia == 6) {
            tituloMisterios = resources.getString(R.string.misterioGozoso)
            tipoMisterio = 1
            misterio1 = resources.getString(R.string.gozoso1)
            meditacionMisterio1 = resources.getString(R.string.meditacionGozosos1)

            misterio2 = resources.getString(R.string.gozoso2)
            meditacionMisterio2 = resources.getString(R.string.meditacionGozosos2)

            misterio3 = resources.getString(R.string.gozoso3)
            meditacionMisterio3 = resources.getString(R.string.meditacionGozosos3)

            misterio4 = resources.getString(R.string.gozoso4)
            meditacionMisterio4 = resources.getString(R.string.meditacionGozosos4)

            misterio5 = resources.getString(R.string.gozoso5)
            meditacionMisterio5 = resources.getString(R.string.meditacionGozosos5)
        }

        if (dia == 2 || dia == 5) {
            tituloMisterios = resources.getString(R.string.misterioDoloroso)
            tipoMisterio = 2
            misterio1 = resources.getString(R.string.doloroso1)
            meditacionMisterio1 = resources.getString(R.string.meditacionDoloroso1)

            misterio2 = resources.getString(R.string.doloroso2)
            meditacionMisterio2 = resources.getString(R.string.meditacionDoloroso2)

            misterio3 = resources.getString(R.string.doloroso3)
            meditacionMisterio3 = resources.getString(R.string.meditacionDoloroso3)

            misterio4 = resources.getString(R.string.doloroso4)
            meditacionMisterio4 = resources.getString(R.string.meditacionDoloroso4)

            misterio5 = resources.getString(R.string.doloroso5)
            meditacionMisterio5 = resources.getString(R.string.meditacionDoloroso5)
        }

        if (dia == 4) {
            tituloMisterios = resources.getString(R.string.misterioLuminoso)
            tipoMisterio = 4
            misterio1 = resources.getString(R.string.luminoso1)
            meditacionMisterio1 = resources.getString(R.string.meditacionLuminoso1)

            misterio2 = resources.getString(R.string.luminoso2)
            meditacionMisterio2 = resources.getString(R.string.meditacionLuminoso2)

            misterio3 = resources.getString(R.string.luminoso3)
            meditacionMisterio3 = resources.getString(R.string.meditacionLuminoso3)

            misterio4 = resources.getString(R.string.luminoso4)
            meditacionMisterio4 = resources.getString(R.string.meditacionLuminoso4)

            misterio5 = resources.getString(R.string.luminoso5)
            meditacionMisterio5 =resources.getString(R.string.meditacionLuminoso5)
        }
    }

    private fun activaSonido() {
        var cadena: String?
        val misterio = resources.getStringArray(R.array.misterios)

        when (voyMisterio) {
            0 -> {
                habla(resources.getString(R.string.inicioRosario))
            }

            1 -> {
                if (avemaria == 0) {
                    cadena = misterio[voyMisterio] + misterio1
                    if (meditados) cadena += meditacionMisterio1
                    colaHabla(cadena)
                }
                rellenaInicio()
            }

            2 -> {
                if (avemaria == 0) {
                    cadena = misterio[voyMisterio] + misterio2
                    if (meditados) cadena += meditacionMisterio2
                    colaHabla(cadena)
                }
                rellenaFin()
            }

            3 -> {
                if (avemaria == 0) {
                    cadena = misterio[voyMisterio] + misterio3
                    if (meditados) cadena += meditacionMisterio3
                    colaHabla(cadena)
                }
                rellenaInicio()
            }

            4 -> {
                if (avemaria == 0) {
                    cadena = misterio[voyMisterio] + misterio4
                    if (meditados) cadena += meditacionMisterio4
                    colaHabla(cadena)
                }
                rellenaFin()
            }

            5 -> {
                if (avemaria == 0) {
                    cadena = misterio[voyMisterio] + misterio5
                    if (meditados) cadena += meditacionMisterio5
                    colaHabla(cadena)
                }
                rellenaInicio()

                val letanias = resources.getStringArray(R.array.letanias)
                letania(resources.getString(R.string.tituloLetanias))
                for (i in letanias.indices) {
                    letania(letanias[i])
                }

                letania(resources.getString(R.string.oracionFinal))
                letania(resources.getString(R.string.intenciones), 9)
            }
        }
    }

    private fun letania(texto: String, id:Int =0) {
        colaHabla(texto, id)
        silencio(400)
    }

    private fun ponAveMaria(am: Int) {
        binding.numeroAveMaria.text = am.toString()
    }

    private fun rellenaContenido(voy: Int) {
        var cadena: String

        voyMisterio = voy

        binding.scrollRosario.scrollY = 0

        cadena = resources.getString(R.string.misteriosHoy) + " " + tituloMisterios
        binding.tipoMisterios.text = cadena

        val misterio = resources.getStringArray(R.array.misterios)
        binding.idTituloRosario.text = misterio[voy]
        binding.idTituloRosario2.visibility = View.VISIBLE
        avemaria = 0

        when (voy) {
            0 -> {
                binding.idTituloRosario2.visibility = View.GONE
                binding.idImagen.visibility = View.GONE
                cadena = resources.getString(R.string.inicioRosario)

                binding.idMeditacion.text = ""
                binding.cajaMeditados.visibility = View.GONE
                binding.idContenidoRosario.text = cadena
                binding.idContenidoRosario2.visibility = View.GONE
            }

            1 -> {
                binding.idImagen.visibility = View.VISIBLE
                binding.idImagen.setImageDrawable(obtenImagen(1, tipoMisterio))
                if (meditados) {
                    binding.idMeditacion.text = meditacionMisterio1
                    binding.cajaMeditados.visibility = View.VISIBLE
                } else binding.cajaMeditados.visibility = View.GONE

                binding.idTituloRosario2.text = misterio1
                cadena = rellenaPNyAM()
                binding.idContenidoRosario.text = cadena
                cadena = resources.getString(R.string.gloria) + "\n\n" + resources.getString(R.string.gloria1) + " " + resources.getString(R.string.gloria2) + "\n\n" + resources.getString(R.string.jesusMio)
                  
                binding.idContenidoRosario2.text = cadena
                binding.idContenidoRosario2.visibility = View.VISIBLE
            }

            2 -> {
                if (meditados) {
                    binding.cajaMeditados.visibility = View.VISIBLE
                    binding.idMeditacion.text = meditacionMisterio2
                } else binding.cajaMeditados.visibility = View.GONE
                binding.idImagen.setImageDrawable(obtenImagen(2, tipoMisterio))
                binding.idTituloRosario2.text = misterio2
                cadena = rellenaPNyAM()
                binding.idContenidoRosario.text = cadena
                cadena = resources.getString(R.string.gloria) + "\n\n" + resources.getString(R.string.gloria1) + resources.getString(R.string.gloria2) + "\n\n" + resources.getString(R.string.jesusMio)

                binding.idContenidoRosario2.text = cadena
                binding.idContenidoRosario2.visibility = View.VISIBLE
            }

            3 -> {
                if (meditados) {
                    binding.cajaMeditados.visibility = View.VISIBLE
                    binding.idMeditacion.text = meditacionMisterio3
                } else binding.cajaMeditados.visibility = View.GONE

                binding.idImagen.setImageDrawable(obtenImagen(3, tipoMisterio))
                binding.idTituloRosario2.text = misterio3
                cadena = rellenaPNyAM()
                binding.idContenidoRosario.text = cadena
                cadena = resources.getString(R.string.gloria) + "\n\n" + resources.getString(R.string.gloria1) + resources.getString(R.string.gloria2) + "\n\n" + resources.getString(R.string.jesusMio)

                binding.idContenidoRosario2.text = cadena
                binding.idContenidoRosario2.visibility = View.VISIBLE
            }

            4 -> {
                if (meditados) {
                    binding.cajaMeditados.visibility = View.VISIBLE
                    binding.idMeditacion.text = meditacionMisterio4
                } else binding.cajaMeditados.visibility = View.GONE

                binding.idImagen.setImageDrawable(obtenImagen(4, tipoMisterio))
                binding.idTituloRosario.text = cadena
                rellenaPNyAM()

                binding.idTituloRosario2.text = misterio4
                cadena = resources.getString(R.string.gloria) + "\n\n" + resources.getString(R.string.gloria1) + resources.getString(R.string.gloria2) + "\n\n" + resources.getString(R.string.jesusMio)

                binding.idContenidoRosario2.text = cadena
                binding.idContenidoRosario2.visibility = View.VISIBLE
            }

            5 -> {
                if (meditados) {
                    binding.cajaMeditados.visibility = View.VISIBLE
                    binding.idMeditacion.text = meditacionMisterio5
                } else binding.cajaMeditados.visibility = View.GONE

                binding.idImagen.setImageDrawable(obtenImagen(5, tipoMisterio))
                binding.idTituloRosario2.text = misterio5
                cadena = rellenaPNyAM()
                binding.idContenidoRosario.text = cadena
                cadena = resources.getString(R.string.gloria) + "\n\n" + resources.getString(R.string.gloria1) + resources.getString(R.string.gloria2) + "\n\n" + resources.getString(R.string.jesusMio)

                val respuestaLetanias = resources.getStringArray(R.array.respuestaLetanias)
                val letanias = resources.getStringArray(R.array.letanias)

                for (i in 0..< letanias.size) {
                    cadena += letanias[i] + " " + respuestaLetanias[i] + "\n"
                }

                cadena += resources.getString(R.string.oracionFinal) + "\n\n" + resources.getString(R.string.intenciones)

                binding.idContenidoRosario2.text = cadena
                binding.idContenidoRosario2.visibility = View.VISIBLE
            }
        }
    }

    private fun obtenImagen(misterio: Int, titulo: Int): Drawable? {
        var imagen: Drawable? = AppCompatResources.getDrawable(this, R.drawable.gozoso1) //resources.getDrawable(R.drawable.gozoso1)
        when (titulo) {
            1 -> when (misterio) {
                1 -> imagen = AppCompatResources.getDrawable(this, R.drawable.gozoso1)
                2 -> imagen = AppCompatResources.getDrawable(this, R.drawable.gozoso2)
                3 -> imagen = AppCompatResources.getDrawable(this, R.drawable.gozoso3)
                4 -> imagen = AppCompatResources.getDrawable(this, R.drawable.gozoso4)
                5 -> imagen = AppCompatResources.getDrawable(this, R.drawable.gozoso5)
            }

            2 -> when (misterio) {
                1 -> imagen = AppCompatResources.getDrawable(this, R.drawable.doloroso1)
                2 -> imagen = AppCompatResources.getDrawable(this, R.drawable.doloroso2)
                3 -> imagen = AppCompatResources.getDrawable(this, R.drawable.doloroso3)
                4 -> imagen = AppCompatResources.getDrawable(this, R.drawable.doloroso4)
                5 -> imagen = AppCompatResources.getDrawable(this, R.drawable.doloroso5)
            }

            3 -> when (misterio) {
                1 -> imagen = AppCompatResources.getDrawable(this, R.drawable.glorioso1)
                2 -> imagen = AppCompatResources.getDrawable(this, R.drawable.glorioso2)
                3 -> imagen = AppCompatResources.getDrawable(this, R.drawable.glorioso3)
                4 -> imagen = AppCompatResources.getDrawable(this, R.drawable.glorioso4)
                5 -> imagen = AppCompatResources.getDrawable(this, R.drawable.glorioso5)
            }

            4 -> when (misterio) {
                1 -> imagen = AppCompatResources.getDrawable(this, R.drawable.luminoso1)
                2 -> imagen = AppCompatResources.getDrawable(this, R.drawable.luminoso2)
                3 -> imagen = AppCompatResources.getDrawable(this, R.drawable.luminoso3)
                4 -> imagen = AppCompatResources.getDrawable(this, R.drawable.luminoso4)
                5 -> imagen = AppCompatResources.getDrawable(this, R.drawable.luminoso5)
            }
        }
        return imagen
    }

    private fun rellenaPNyAM(): String {
        return resources.getString(R.string.tituloPadreNuestro) + "\n\n" + resources.getString(R.string.padreNuestro)
    }

    private fun rellenaInicio() {
        if (avemaria == 0) {
            colaHabla(resources.getString(R.string.padreNuestro1))
            silencio(9000)
        }
        for (i in avemaria..9) {
            colaHabla(resources.getString(R.string.aveMaria1), 1)
            silencio(7000)
        }
        colaHabla(resources.getString(R.string.gloria1))
        silencio(3000)
        colaHabla(resources.getString(R.string.jesusMio))
    }

    private fun rellenaFin() {
        if (avemaria == 0) {
            silencio(9000)
            colaHabla(resources.getString(R.string.padreNuestro2))
        }
        for (i in avemaria..9) {
            silencio(7000)
            if (i == 9) colaHabla(resources.getString(R.string.gloria))
            colaHabla(resources.getString(R.string.aveMaria2), 2)
        }
        silencio(3000)
        colaHabla(resources.getString(R.string.gloria2) + resources.getString(R.string.jesusMio))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 2) {
            if (Build.VERSION.SDK_INT >= 33) {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        resources.getString(R.string.permisos),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun velocidadLectura(vel: Int): Float {
        return when (vel) {
            0-> 0.3f
            1-> 0.5f
            2-> 0.7f
            3-> 1.0f
            4-> 1.4f
            5 -> 1.8f
            else -> {1f}
        }
    }

    private fun habla(text: String) {
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utteranceId")

        val velocidad = velocidadLectura(lento)
        textToSpeech.setSpeechRate(velocidad)

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utteranceId")
    }

    private fun colaHabla(text: String, id: Int = 0) {
        val params =  HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = id.toString()

        val velocidad = velocidadLectura(lento)

        textToSpeech.setSpeechRate(velocidad)
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params)
    }

    private fun silencio(duracion: Long) {
        val pausa = 3-lento
        var duracionPausa: Long = 0

        if (pausa<0)
            duracionPausa = duracion / (-pausa)
        else if (pausa>0)
            duracionPausa = duracion * pausa
        else duracionPausa = duracion

        textToSpeech.playSilentUtterance(duracionPausa, TextToSpeech.QUEUE_ADD, null)
    }

    private fun permisos() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                ) Toast.makeText(this, resources.getString(R.string.rechazaPermisos), Toast.LENGTH_LONG).show()

                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
            }
        }
    }

    private fun borraAlarma(){
        val sharedPreferences = getSharedPreferences("mis_preferencias", Context.MODE_PRIVATE)

        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        val i = Intent(this, Alarma::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            i,
            PendingIntent.FLAG_IMMUTABLE
        )

        am.cancel(pendingIntent)

        val editor = sharedPreferences.edit()
        editor.putBoolean("tarea_programada", false)
        editor.apply()
    }

    private fun alarma() {
        val sharedPreferences = getSharedPreferences("mis_preferencias", Context.MODE_PRIVATE)
        val tareaProgramada = sharedPreferences.getBoolean("tarea_programada", false)

        if (!tareaProgramada) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager

            val i = Intent(this, Alarma::class.java)

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                i,
                PendingIntent.FLAG_IMMUTABLE
            )

            am.cancel(pendingIntent)

            val cal = android.icu.util.Calendar.getInstance()
            cal.timeInMillis = System.currentTimeMillis()
            //var dia = cal[android.icu.util.Calendar.DAY_OF_WEEK]++
            //if (dia == 8) dia = 1

            //cal[android.icu.util.Calendar.DAY_OF_WEEK] = dia

            am.setRepeating(AlarmManager.RTC_WAKEUP,
                cal.timeInMillis + (24*60*60*1000),  24* 60 * 60 * 1000 , pendingIntent)
        }

        val editor = sharedPreferences.edit()
        editor.putBoolean("tarea_programada", true)
        editor.apply()
    }
}