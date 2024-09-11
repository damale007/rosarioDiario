package com.conadasoft.rosariodiario

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.conadasoft.rosariodiario.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdRequest
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

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
    private var voz: Boolean = false
    private var avemaria = 0
    private var meditados = false
    private var estaHablando = false
    private val adRequest = AdRequest.Builder().build()


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
            }
        }

        val pref = PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )
        meditados = pref.getBoolean("meditados", false)
        val notificaciones = pref.getBoolean("notificacion", true)

        if (notificaciones)
            alarma()
        else {
            WorkManager.getInstance(applicationContext).cancelUniqueWork("daily_alarm")
            val sharedPreferences = getSharedPreferences("mis_preferencias", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("tarea_programada", false)
            editor.apply()
        }

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
        avemaria = 0
        voz = false

        binding.adView.loadAd(adRequest)

        if (voyMisterio == 0) binding.contadorAveMarias.visibility = View.GONE

        asignaMisterios()
        rellenaContenido(0)

        eventos()
    }

    private fun eventos() {
        binding.settings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        binding.botonSiguienteMisterio.setOnClickListener {
            val misterio = arrayOf(
                "Inicio",
                "Primer Misterio",
                "Segundo Misterio",
                "Tercer Misterio",
                "Cuarto Misterio",
                "Quinto Misterio",
                "Letanías"
            )
            voyMisterio++
            if (voyMisterio == 6) voyMisterio = 0
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

            binding.misterioNumero.text = misterio[voyMisterio]
            if (voz) activaSonido()
            binding.adView.loadAd(adRequest)
        }

        binding.botonSiguienteAveMaria.setOnClickListener {
            avemaria++
            ponAveMaria(avemaria)
            if (avemaria == 10) binding.botonSiguienteAveMaria.isEnabled = false
        }

        binding.leeme.setOnClickListener {
            if (estaHablando) {
                binding.leeme.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_leer, 0, 0, 0)
                textToSpeech.stop()
                voz = false
            } else {
                binding.leeme.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_stop, 0, 0, 0)
                voz = true
                activaSonido()
            }
            estaHablando = !estaHablando
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
        val notificaciones = pref.getBoolean("notificacion", true)

        if (notificaciones)
            alarma()
        else {
            WorkManager.getInstance(applicationContext).cancelUniqueWork("daily_alarm")
            val sharedPreferences = getSharedPreferences("mis_preferencias", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("tarea_programada", false)
            editor.apply()
        }

        asignaMisterios()
        rellenaContenido(voyMisterio)
    }

    private fun asignaMisterios() {
        val now = Calendar.getInstance()
        val dia = now[Calendar.DAY_OF_WEEK] - 1

        if (dia == 0 || dia == 3) {
            tituloMisterios = "gloriosos"
            misterio1 = "La Resurrección del Señor"
            meditacionMisterio1 = """
                «El primer día de la semana, muy de mañana, fueron al sepulcro llevando los aromas que habían preparado. Pero encontraron que la piedra había sido retirada del sepulcro, y entraron, pero no hallaron el cuerpo del Señor Jesús. No sabían qué pensar de esto, cuando se presentaron ante ellas dos hombres con vestidos resplandecientes. Ellas, despavoridas, miraban al suelo, y ellos les dijeron: "¿Por qué buscáis entre los muertos al que está vivo? No está aquí, ha resucitado"» (Lc 24, 1-6).
                
                Reflexión
                A la luz del misterio nuestra fe contempla vivientes, unidas ya para siempre a Jesucristo resucitado, las almas que nos fueron más queridas, de cuya familiaridad gozamos, cuyas penas compartimos. ¡Cómo se aviva en el corazón, al calor del misterio de la resurrección, el recuerdo de nuestros muertos! Recordados y favorecidos con el sufragio del sacrificio del Señor crucificado y resucitado, toman parte aún en lo mejor de nuestra vida, la oración y Jesucristo.
                
                Intención
                Por algo la liturgia oriental termina los ritos fúnebres con el aleluya por todos los muertos. Pidamos para ellos la luz de las moradas eternas, mientras el pensamiento se detiene en la resurrección que aguarda a nuestros propios restos mortales: “Espero en la resurrección de los muertos”. El saber esperar. El confiar siempre en la suavísima promesa, de la que es prenda la resurreción de Cristo, es ciertamente un cielo anticipado.
                """.trimIndent()
            misterio2 = "La Ascensión del Señor"
            meditacionMisterio2 = """
                «El Señor Jesús, después de hablarles, ascendió al cielo y se sentó a la derecha de Dios» (Mc 16, 19).
                
                Reflexión
                Este momento del Rosario nos enseña y exhorta a que no nos dejemos prender en lo que pesa y entorpece, abandonándonos, en cambio, a la voluntad del Señor, que nos estimula hacia lo alto. En el momento de volver al Padre, subiendo al cielo, los brazos del Señor se abren bendiciendo a los primeros apóstoles, y alcanza a todos los que, siguiendo sus huellas, siguen creyendo en Él, y es para sus almas una plácida y serena seguridad del encuentro definitivo con Él y todos los salvados en la felicidad eterna.
                
                Intención
                Ante todo, el misterio e nos presenta como luz y norma para las almas que se preocupan de su propia vocación. En lo íntimo del misterio se halla el movimiento de vida espiritual, el deseo ardiente de superación continua, que arde en el corazón de los sacerdotes no apegados a ls cosas de la tierra, cuidadosos únicamente de abrirse, y abrir a otros, caminos que llevan a la perfección y santidad, al grado de gracia a que deben llegar, en privado o en común: sacerdotes, religiosos y religiosas, misioneros y misioneras, seglares amantes de Dios y de su Iglesia, y muchas almas, aquellas al menos que son como “el buen olor de Cristo”, junto a las cuales se siente cercano al Señor. Viven, en efecto, ya ahora, en una comunión constante de vida celestial.
                
                
                """.trimIndent()
            misterio3 = "La venida del Espíritu Santo sobre María y los apóstoles."
            meditacionMisterio3 = """
                «Al llegar el día de Pentecostés, estaban todos reunidos en un mismo lugar. De repente vino del cielo un ruido como el de una ráfaga de viento impetuoso, que llenó toda la casa en la que se encontraban. Se les aparecieron unas lenguas como de fuego que se repartieron y se posaron sobre cada uno de ellos; quedaron todos llenos del Espíritu Santo y se pusieron a hablar en otras lenguas, según el Espíritu les concedía expresarse» (Hch 2, 1-4).
                
                Reflexión
                La virtud divina que infunde el Espíritu Santo en el alma de los hombres es gran apoyo de la espeanza, fuerza poderosa, única ayuda verdadera para la vida humana. Nos referimos a la gracia que nos santifica, y que en realidad es precedida y seguida de gracias efectivas. Ciertamente lo que importa grandemente es el que el espíritu de los hombres se renueve en su interior, naciendo a nueva vida.
                
                Intención
                María, la Madre de Jesús, y siempre dulce Madre nuestra, se hallaba con los apóstoles en el cenáculo de Pentecostés. Permanezcamos muy cerca de ella por medio del Rosario. Nuestras oraciones unidas a las suyas renovarán el antiguo prodigio. Será como el nacimiento de un nuevo día, un alba esplendorosa en la Iglesia católica, santa y aún más santa, católica y aún más católica, en los tiempos modernos.
                """.trimIndent()
            misterio4 = "La Asunción de Nuestra Señora a los Cielos"
            meditacionMisterio4 = """
                «Todas las generaciones me llamarán bienaventurada porque el Señor ha hecho obras grandes en mí» (Lc 1, 48-49).
                
                Reflexión
                Es motivo de consuelo y confianza, en los días de dolor, para las almas privilegiadas –y todos podemos serlo, a condición de ser fieles a la gracia- que Dios prepara en el silencio al triunfo más bello, al triunfo del altar.
                
                Intención
                El misterio de la Asunción nos hace familiar el pensamiento de la muerte, de nuestra muerte, y es una invitación al abandono confiado. Nos familiariza y hace amigos de la idea de que el Señor estará presente en nuestra agonía, como querríamos que estuviese, para tomar Él en sus manos nuestra alma inmortal.
                
                ¡Virgen Inmaculada: que podamos compartir contigo la gloria celestial!
                """.trimIndent()
            misterio5 = "La Coronación de María como reina de todo lo creado"
            meditacionMisterio5 = """
                «Una gran señal apareció en el cielo: una mujer, vestida de sol, con la luna bajo sus pies, y una corona de doce estrellas sobre su cabeza» (Ap 12, 1).
                
                Reflexión
                La reflexión ha de recaer sobre nosotros mismos; sobre nuestra vocación por la que un día seremos asociados a los ángeles y a los santos y cuyas gracias santificantes anticipan ya desde esta vida la realidad mistreriosa y consoladora; ¡oh qué delicia, oh qué gloria! Somos “conciudadanos de los santos y de la familia de Dios; edificados sobre el fundamento de los apóstoles y de los profetas, siendo piedra angular el mismo Cristo Jesús”.
                
                Intención
                La intención de este misterio es orar por la perseverancia final y por la paz sobre la tierra, que abre las puertas de la eternidad bienaventurada.
                
                Oh María, tú que ruegas con nosotros, tú que ruegas por nosotros. Lo sabemos. Lo sentimos, oh qué realidad más deliciosa, qué gloria más soberana, en esta concordia celestial y humana de afectos, de palabras, de vida, que nos ha procurado y procura el Rosario: mitigación del dolor, prueba sabrosa de paz celestial, esperanza de vida eterna.
                """.trimIndent()
        }

        if (dia == 1 || dia == 6) {
            tituloMisterios = "gozosos"
            misterio1 = "La encarnación del Hijo de Dios"
            meditacionMisterio1 = """
                «Al sexto mes el ángel Gabriel fue enviado por Dios a una ciudad de Galilea, llamada Nazaret, a una virgen desposada con un hombre llamado José, de la estirpe de David; el nombre de la virgen era María» (Lc 1,26-27). 
                
                Reflexión
                Reflexionando sobre esto, nuestro primer deber inolvidable es dar gracias a Dios, porque se ha dignado venir a salvarnos. Por esto se ha hecho hombre, hermano nuestro. Igual a nosotros en cuanto a nacer de una mujer, de la que nos ha hecho hijos de adopción al pie de la cruz. Hijos adoptivos de su Padre celestial, ha querido que lo seamos igualmente de su misma madre.
                
                Intención
                Sea la intención de nuestra oración, al contemplar este primer misterio que se nos ofrece a la meditación, además de dar gracias continuamente, un esfuerzo, en verdad sincero y leal, de humildad, de pureza, de caridad, virtudes de las que nos da tan alto ejemplo la Virgen bendita.
                """.trimIndent()
            misterio2 = "La visitación de Nuestra Señora a Santa Isabel"
            meditacionMisterio2 =
                """«En aquellos días María se puso en camino y fue aprisa a la región montañosa, a una ciudad de Judá; entró en casa de Zacarías y saludó a Isabel. 

Refelxión
Y sucedió que, en cuanto Isabel oyó el saludo de María, saltó de gozo el niño en su seno, e Isabel quedó llena de Espíritu Santo; y exclamando a voz en grito, dijo: "Bendita tú entre las mujeres y bendito el fruto de tu seno"» (Lc 1, 39-42)
 Reflexión
Cuanto sucede aquí, en Ain-Karem, en el monte Hebrón, presenta, con luz celeste y al mismo tiempo muy humana, qué relaciones son las que unen entre sí a las buenas familias cristianas, educadas en la antigua escuela del Rosario. Rosario recitado cada noche en casa, en el círculo de los íntimos. Rosario recitado, no en una ni en cien, ni en mil familias, sino por todas y por todos, y en todos los lugares de la tierra, allí donde uno cualquiera de nosotros “sufre, lucha y ora”, fiel a una inspiración de lo alto, como el sacerdocio, la caridad misionera, la prosecución de un ideal de apostolado; o también por fidelidad a uno de aquello motivos, tan legítimos que llegan a ser obligatorios, como el trabajo, el comercio, el servicio militar, el estudio, la enseñanza, o cualquier otra ocupación.

Intención
Bello es confundirse durante las diez avemarías del misterio con tantas y tantas almas, unidas por vínculos de sangre, o domésticos, en una relación que santifica y por lo mismo consolida el amor de las personas amadas: con padres e hijos, hermanos y parientes, vecinos y compatriotas. Todo esto, con la finalidad y el propósito vivido de sostener, aumentar y hacer más viva la presencia de la caridad con todos, cuyo ejercicio proporciona la alegría más profunda y es el mayor honor de la vida."""
            misterio3 = "El nacimiento del Hijo de Dios"
            meditacionMisterio3 = """
                «Sucedió que por aquellos días salió un edicto de César Augusto ordenando que se empadronase todo el mundo. Este primer empadronamiento tuvo lugar siendo Cirino gobernador de Siria. Iban todos a empadronarse, cada uno a su ciudad. 
                Subió también José desde Galilea, de la ciudad de Nazaret, a Judea, a la ciudad de David, que se llama Belén, por ser él de la casa y familia de David, para empadronarse con María, su esposa, que estaba encinta. Y sucedió que, mientras ellos estaban allí, se le cumplieron los días del alumbramiento, y dio a luz a su hijo primogénito, le envolvió en pañales y le acostó en un pesebre, porque no tenían sitio en el alojamiento» (Lc 2,1-7).
                
                Reflexión
                En este misterio no quede una sola rodilla sin doblarse ante la cuna, en gesto de adoración. Nadie se quede sin ver los ojos del divino Niño que miran lejos, como queriendo ver, uno a uno, todos los pueblos de la tierra. Van pasando uno a uno ante su presencia, como en una revista, y los reconoce a todos: hebreos, romanos, griegos, chinos, indios, pueblos de África, de cualquier región de la tierra, o época de la historia. Las regiones más distantes y desérticas, las más remotas e inexploradas; los tiempos pasados, el presente, y los tiempos por venir.
                
                Intención
                Al Santo Padre, en el transcurso de las diez Avemarías, le gusta encomendar a Jesús que nace, el incontable número de niños -¡cuántos son!, muchedumbre interminable- que han nacido en las últimas veinticuatro horas, de día o de noche, de la raza que sean, aquí y allí, un poco por toda la tierra. ¡Cuántos son! Todos ellos pertenecen, de derecho, bautizados o no, a Jesús, el niño que acaba de nacer en Belén. Están llamados al reconocimiento de su dominio, que es el mayor y más dulce que pueda darse en el corazón del hombre, o en las historia del mundo: único dominio digno de Dios y de los hombres. Reino de luz y de paz, el reino que pedimos en el Padrenuestro.
                """.trimIndent()
            misterio4 = "La Presentación del Señor Jesús en el templo"
            meditacionMisterio4 =
                """«Cuando se cumplieron los ocho días para circuncidarle, se le dio el nombre de Jesús, como lo había llamado el ángel antes de ser concebido en el seno. Cuando se cumplieron los días de la purificación de ellos, según la Ley de Moisés, llevaron a Jesús a Jerusalén para presentarle al Señor, como está escrito en la Ley del Señor: Todo varón primogénito será consagrado al Señor y para ofrecer en sacrificio un par de tórtolas o dos pichones, conforme a lo que se dice en la Ley del Señor» (Lc 2, 21-24).
Reflexión e intención

            De manera diferente, pero semejante en cuanto al sentido de la ofrenda, el episodio se renueva continuamente en la Iglesia, o por mejor decir, es algo constante en ella. Será muy grato contemplar, durante las diez Avemarías, el campo que germina, la cosecha que se alza. “Mirad los campos que ya están amarillos para la siega”. Me refiero a la alegre esperanza que se ve nacer del sacerdocio, de sus cooperadores y cooperadoras, tan numerosos en el reino de Dios, y sin embargo no suficientes aún. Jóvenes del seminario, de las casas religiosas, seminarios de misiones, y aun en las universidades católicas. ¿Por qué no aquí, si son cristianos, llamados también ellos a ser apóstoles? Y la alegre esperanza de tantas iniciativas de apostolado de los seglares, imprescindibles en el mañana. Apostolado que, no obstante las dificultades y pruebas de su expansión, ofrece, y jamás dejará de ofrecer, un espectáculo tan conmovedor que arranca palabras de alegría y admiración.

Luz y revelación de las gentes, gloria de pueblo elegido."""
            misterio5 = "Pérdida del Niño Jesús y su hallazgo en el templo"
            meditacionMisterio5 = """
                «Sus padres iban todos los años a Jerusalén a la fiesta de la Pascua. Cuando tuvo doce años, subieron ellos como de costumbre a la fiesta y, al volverse, pasados los días, el niño Jesús se quedó en Jerusalén, sin saberlo sus padres... 
                Y sucedió que al cabo de tres días, le encontraron en el Templo sentado en medio de los maestros, escuchándoles y preguntándoles; todos los que le oían, estaban estupefactos por su inteligencia y sus respuestas» (Lc 2, 41-47)
                
                Reflexión
                El deber de la inteligencia humana es el mismo en todo tiempo: recoger la sabiduría del pasado, transmitir la buena doctrina, hacer avanzar, con firmeza y humildad, la investigación científica. Nosotros morimos uno tras otro. Vamos hacia Dios. La humanidad, en cambio, mira al porvenir.
                
                Cristo no está jamás ausente, ni del conocimiento sobrenatural, ni en el ámbito del natural. Está siempre en el juego, en su puesto. “Uno solo es vuestro maestro, Cristo”.
                
                Intención
                Ésta, que es la quinta decena, última de los misterios gozosos, reservémosla, con una intención especialísima, a favor de todos aquellos que han sido llamados por Dios –por su capacidad natural, por circunstancias de la vida, por voluntad de sus superiores- al servicio de la verdad: en la investigación o la enseñanza, difundiendo el saber antiguo, o las técnicas nuevas, mediante libros o técnicas audiovisuales. Todos ellos están llamados a imitar a Jesucristo: los intelectuales, profesores, periodistas. Todos, especialmente los periodistas, a quienes incumbe diariamente la tarea peculiarísima de hacer honor a la verdad, deben transmitirla con religiosa escrupulosidad, con agudo buen sentido, sin  distorsionarla ni desfigurarla con fantasías.
                
                Si, sí, recemos por todos ellos: recemos por ellos, sean sacerdotes o seglares; para que sepan escuchar la verdad; y cuánta pureza de corazón se necesita para que sepan comprenderla; y cuánta humildad íntima de pensamiento es necesaria para que sepan defenderla, ya que desde entonces se hace inevitable la obediencia, que fue la fuerza de Jesús, y es la fuerza de los santos. Sólo la obediencia obtiene la paz, es decir, la victoria.
                """.trimIndent()
        }

        if (dia == 2 || dia == 5) {
            tituloMisterios = "dolorosos"
            misterio1 = "La Oración de Jesús  en el Huerto de Getsemaní"
            meditacionMisterio1 = """
                «Entonces Jesús fue con ellos a un huerto, llamado Getsemaní, y dijo a sus discípulos: "Sentaos aquí mientras voy a orar". Y tomando consigo a Pedro y a los dos hijos de Zebedeo, comenzó a sentir tristeza y angustia. Entonces les dijo: "Mi alma está triste hasta el punto de morir; quedaos aquí y velad conmigo". Y adelantándose un poco, cayó rostro en tierra, y suplicaba así: "Padre mío, si es posible, que pase de mí esta copa, pero no sea como yo quiero, sino como quieras tú"» (Mt 26, 36-39).
                
                Reflexión
                La escena de Getsemaní nos conforta y anima a realizar un esfuerzo voluntario de aceptación. La aceptación incondicional del sufrimiento, cuando es Dios quien lo quiere o permite: “No se haga mi voluntad, sino la tuya”. Palabras que desgarran y curan, porque enseñan a qué grado de fervor puede y debe llegar el cristiano que sufre, unido a Cristo que sufre. Ellas nos dan, como en última pincelada, la certeza de méritos inefables, el merecimiento de la vida divina para nosotros, vida palpitante hoy en nosotros por la gracia, mañana en la gloria.
                
                Intención
                En este misterio se presenta ante nuestra mirada una intención  particular: “la preocupación por todas las Iglesias”. Solicitud que impulsa con apremio la oración diaria del Santo Padre, como el viento que azotaba el lago de Genesaret, “viento contrario”. Pensamiento anhelante en las situaciones más comprometidas de su altísimo ministerio pastoral. Preocupación por la Iglesia, que esparcida por la redondez de la tierra, sufre unida a él, y él, por su parte, unido a ella, presente en él y sufriendo con él. Afán dolorido por tantas almas, porciones enteras del rebaño de Cristo, sujetas a persecución, sin libertad de creer, de pensar, de vivir. “¿Quién desfallece que no desfallezca yo?”
                
                Participar en el dolor del prójimo, padecer con quien padece, llorar con quien llora es un beneficio, un mérito para toda la Iglesia. La “comunión de los santos” es este tener en común, todos y cada uno, la Sangre de Cristo, el amor de los santos y de los buenos, y, también, Dios mío, nuestros pecados, nuestras debilidades. ¿Se piensa lo suficiente en esta “comunión”, que es unión, y, como diría Jesucristo, casi unidad, “que sean uno”? La cruz del Señor no sólo nos eleva a nosotros, sino que atrae a las almas. Siempre. “Y yo, cuando fuere levantado de la tierra, atraeré todos a mí”. Todo. A todos.
                
                
                """.trimIndent()
            misterio2 = "La Flagelación del Señor"
            meditacionMisterio2 = """
                «Pilato puso en libertad a Barrabás; y a Jesús, después de haberlo hecho azotar, lo entregó para que fuera crucificado» (Mt 27, 26).
                
                Reflexión
                De aquí se desprende una valiosa enseñanza para todos. No estaremos llamados al martirio sangriento; pero a la disciplina constante y a la diaria mortificación de las pasiones, sí. Por este medio, verdadero “via crucis” de cada día, inevitable, indispensable, que en ocasiones puede incluso llegar a ser heroico en sus exigencias, se llega paso a paso a una semejanza cada vez más estrecha con Jesucristo, a la participación en sus méritos, a la ablución por su sangre inmaculada de todo pecado en nosotros y en los demás. No se llega a esto por fáciles exaltaciones, fanatismo, ojalá inocente, jamás inofensivo.
                
                La Madre, dolorida, lo vio así de flagelado. Pensemos con qué  amargura. Cuántas madres querrían poder gozar del éxito en la perfección de sus hijos, dispuestos, iniciados por ellas en la disciplina de una buena educación, en una vida sana, y en cambio tienen que llorar la pérdida de tantas esperanza, el dolor de que tantos afanes se hayan perdido.
                
                Intención
                En las Avemarías del misterio pediremos al Señor el don de la pureza de costumbres en la familia, en la sociedad, particularmente para los corazones jóvenes, los más expuestos a la seducción de los sentidos. Y juntamente pediremos el don de la firmeza de carácter y de la fidelidad a toda prueba a las enseñanzas recibidas, a los propósitos hechos.
                """.trimIndent()
            misterio3 = "La Coronación de espinas"
            meditacionMisterio3 = """
                «Entonces los soldados del procurador llevaron consigo a Jesús al pretorio y reunieron alrededor de él a toda la cohorte. Lo desnudaron y le echaron encima un manto de púrpura y, trenzando una corona de espinas, se la pusieron sobre la cabeza, y en su mano derecha una caña, y doblando la rodilla delante de él, le hacían burla diciendo: "Salve, Rey de los judíos"». (Mt 27, 27-29)
                
                Reflexión
                Es el misterio cuya contemplación se ajusta mejor a aquellos que llevan el peso de graves responsabilidades en el cuidado de las almas y en la dirección del cuerpo social; por tanto, el misterio de los Papas, se los Obispos, de los Párrocos; el misterio de los gobernantes, de los legisladores, de los magistrados. También sobre su cabeza hay una corona en la cual está, sí, una aureola de dignidad y de distinción, pero que por ello mismo pesa y punza, procura espinas y disgustos. Donde está la autoridad no puede faltar la cruz, a veces de la incomprensión, la del desprecio, o la de la indiferencia y la de la soledad. 
                
                Intención
                Podría ser otra aplicación del misterio pensar en la grave responsabilidad de quien por haber recibido más talentos, está por ello mismo, más obligado a hacerlos fructificar con abundancia, mediante el ejercicio constante de sus facultades, de su inteligencia. El servicio del pensamiento, quiero decir, lo que se espera de quien está mejor dotado, como luz y guía de los demás, debe prestarse con paciencia serena, rechazando tentaciones de orgullo, de egoísmo, del distanciamiento que destruye.
                """.trimIndent()
            misterio4 = "Jesús con la cruz a cuestas camino al calvario"
            meditacionMisterio4 = """
                «Y obligaron a uno que pasaba, a Simón de Cirene, que volvía del campo, el padre de Alejandro y de Rufo, a que llevara su cruz. Lo condujeron al lugar del Gólgota, que quiere decir de la "Calavera"» (Mc 15, 21-22).
                Reflexión
                Contemplando a Jesucristo que sube al Calvario, aprendemos, antes con el corazón que con la mente, a abrazarnos y besar la cruz, a llevarla con generosidad, con alegría, según las palabras del Kempis: “En la cruz está la salvación, en la cruz la vida, en la cruz está la defensa contra los enemigos, en ella la infusión de una suavidad soberana”.
                
                ¿Y cómo no extender nuestra oración a María, la Madre dolorosa que siguió a Jesús, con un espíritu de total participación en sus méritos, en sus dolores?
                
                Intención 
                Que el misterio haga pasar ante nuestra mirada el espectáculo inenarrable de tantos seres atribulados: huérfanos, ancianos, enfermos, minusválidos, prisioneros, desterrados. Pidamos para todos ellos la fuerza, el consuelo capaz de dar esperanza. Repitamos con ternura, y, ¿por qué no?, con alguna lágrima escondida: Salve, cruz, única posible esperanza.
                """.trimIndent()
            misterio5 = "La Crucifixión y Muerte de Nuestro Señor"
            meditacionMisterio5 =
                """«Llegados al lugar llamado "La Calavera", le crucificaron allí a él y a los dos malhechores, uno a la derecha y otro a la izquierda. Jesús decía: "Padre, perdónales, porque no saben lo que hacen"... Era ya eso de mediodía cuando, al eclipsarse el sol, hubo oscuridad sobre toda la tierra hasta la media tarde. El velo del Santuario se rasgó por medio y Jesús, dando un fuerte grito dijo: "Padre, en tus manos pongo mi espíritu" y, dicho esto, expiró» (Lc  23, 33-46).

Reflexión
Vida y muerte representan los dos puntos preciosos y orientadores del sacrificio de Cristo: desde la sonrisa de Belén que quiere abrirse a todos los hijos de los hombres en su primera aparición en la tierra, hasta el suspiro final que recoge todos los dolores para santificarnos, todos los pecados para borrarlos. Y María está junto a la cruz, como estaba junto al Niño de Belén.

Recemos a esta piadosa Madre a fin de que Ella misma ruegue por nosotros ahora y en la hora de nuestra muerte.
Aquí está iluminado también el gran misterio de los pecadores obstinados, de los incrédulos, de aquellos que no recibieron ni recibirán la luz del Evangelio, que no sabrán darse cuenta de la sangre vertida por ellos también, por el Hijo de Dios.

Intención
Pensando en esto de la oración se sumerge en un deseo magnánimo, en una vehemencia reparadora, en un horizonte mundial de apostolado. Y se pide, con gran fervor, que la preciosísima Sangre derramada por todos los hombres, dé al fin, y les dé a todos ellos, conversión y salvación. Que la sangre de Cristo sea para todos arras y prenda de vida eterna."""
        }

        if (dia == 4) {
            tituloMisterios = "luminosos"
            misterio1 = "El Bautismo en el Jordán"
            meditacionMisterio1 =
                """«Bautizado Jesús, salió luego del agua; y en esto se abrieron los cielos y vio al Espíritu de Dios que bajaba en forma de paloma y venía sobre él. Y una voz que salía de los cielos decía: "Este es mi Hijo amado, en quien me complazco"». (Mt 3,16-17)
 Podríamos imaginar a Jesús despidiéndose de su Madre para iniciar la misión que le fue encomendada, la cual duraría aproximadamente tres años. Llega al Río Jordán donde sabía que estaría Juan el Bautista preparándole el camino. Juan hacia confesar a la gente sus pecados y los bautizaba con agua para llamar al arrepentimiento y a la conversión. Jesús se presenta en medio de quienes también esperaban su turno para ser bautizados y pide públicamente el ser bautizado. Juan al verlo le dice asombrado “yo soy el que necesito que tu me bautices!”,  reconociendo a Jesús como nuestro salvador delante de todos,  quien viene a bautizarnos con el Espíritu Santo para liberarnos del pecado original y hacernos hijos amados de Dios Padre; pero Jesús lo invita a unirse a El para cumplir la Voluntad de Dios.

Reflexión

Jesús nos muestra su humildad porque aun siendo el Hijo de Dios, se hace bautizar delante de tanta gente, permitiéndonos entender con este hecho la importancia que ha de tener el Sacramento del Bautismo en nuestras vidas. Juan Bautista nos deja el testimonio de haber visto el Espíritu de Dios en forma de paloma posarse sobre Jesús y haber escuchado la voz de un Padre complacido.

En este Evangelio se nos enseña que uniéndonos a Jesús nos unimos al Padre a través del Espíritu Santo, un misterio inexplicable de fe.

Divina Unión, Santa Unión, Misteriosa Unión que nos bendice en el Padre, el Hijo y el Espíritu Santo, ayúdanos a imitar la humildad de Cristo, a crecer como verdaderos hijos de Dios, a obedecer sus mandamientos y a unirnos a El para que se cumpla siempre la Voluntad de Dios."""
            misterio2 = "La autorrevelación en las bodas de Caná"
            meditacionMisterio2 = """
                «Tres días después se celebraba una boda en Caná de Galilea y estaba allí la madre de Jesús. Fue invitado también a la boda Jesús con sus discípulos. Y, como faltara vino, porque se había acabado el vino de la boda, le dice a Jesús su madre: "No tienen vino". Jesús le responde: "¿Qué tengo yo contigo, mujer? Todavía no ha llegado mi hora". Dice su madre a los sirvientes: "Haced lo que él os diga"». (Jn 2, 1-5).
                Podemos imaginar a Jesus, María y a los discípulos de Jesus, entre muchos familiares y amigos cercanos disfrutando de una boda bendecida con la presencia de Jesus. El Señor nos muestra en este Evangelio su complacencia en la santa unión de un hombre y una mujer, porque alli en esa Boda instituye el sacramento del Matrimonio al realizar su primer milagro.
                
                Reflexión
                
                El Evangelio nos dice claramente que “la Madre de Jesus estaba allí”. Si,  la Madre de Jesus siempre esta alli, donde esta Jesus. Ella esta velando por las necesidades de los demas y se preocupa por el bienestar de sus hijos. Nuestra Virgen Maria,  esta junto a Jesus para decirle, “no tienen vino”, no tienen salud, no tienen trabajo, no tienen vida, no tienen esperanza, no tiene fe, si, allí esta nuestra Madre procurando colaborar en los asuntos de Jesus, y con sus palabras humildes nos pide simplemente “Hagan, lo que El les diga”, nos dice claramente confíen, no pierdan la esperanza, entreguense a El y escuchenlo!
                
                Santa Madre de Dios, dulce Virgen María, sabemos que siempre vigilas nuestras necesidades y te adelantas a abogar por nuestras carencias. Y Tu Señor, nos dejas ver claramente en este Evangelio el amor tan grande que sientes por tu Madre y que eres debil a sus peticiones. Así como Tu Señor, deseamos amar a María, y complacerla como Tu lo haces, porque Ella tan solo quiere que hagamos, lo que Tu nos digas.
                """.trimIndent()
            misterio3 = "El anuncio del Reino de Dios invitando a la conversión"
            meditacionMisterio3 = """
                "El tiempo se ha cumplido y el Reino de Dios está cerca; convertíos y creed en el Evangelio". (Mc 1, 15)
                En este Evangelio Jesús nos invita a la conversión. Convertirse significa dejarlo todo para seguir a Jesús. Convertirse significa actuar por Cristo y en Cristo. Convertirse significa descubrir y agradecer las virtudes de la fe, la esperanza y la caridad. Convertirse significa cruzar las fronteras de nuestro egoísmo y ver a Jesús en nuestros hermanos. Convertirse requiere abandonarse a la voluntad de Dios y estar agradecidos incluso en las desavenencias, enfermedades, humillaciones, falsos testimonios y descubrir en ellos que aceptándolos nos permiten demostrarle al Señor cuan inmenso es nuestro amor, porque amamos también lo que El dispone para nosotros y para nuestra salvación.
                
                Reflexión
                
                Convertirse es amar con sinceridad. Convertirse es colocar a Dios en el centro de nuestras vidas y amarlo con todas las fuerzas. Para convertirse es necesario dejar en manos de Dios todo nuestro ser en cuerpo y alma.
                
                Convertirse muchas veces es saber callar para escuchar y saber hablar cuando necesitan de nuestro consuelo. Convertirse es colocar al Señor siempre en primer lugar, convertirse es olvidarse de nuestros intereses y comodidades. Convertirse es ser obedientes, es apartarnos nosotros para que sea el Señor quien protagonice. Convertirse es hablar con Dios primero y luego hablar de El con nuestros hermanos. Convertirse es no temer a ser criticados por dar testimonio de cuanto Jesús nos ama y como ha transformado nuestras vidas.
                
                Convertirse es darse y servir a los demás, es buscar tiempo donde no lo hay y disfrutar de emplearlo para servir a un solo Dios. Convertirse es emplearse como obrero del Señor, declararse peón, servidor, el menos útil de todos. Convertirse es sentirse nada, es vaciarse de uno mismo y llenarse de Dios. Convertirse es reconocer que nuestra conversión y las conversiones de nuestros hermanos son méritos solamente de Dios.
                
                Señor, convierte nuestras almas en la tuya, para predicar tu Palabra con humildad, para ser solo instrumentos inútiles en la construcción de Tu Reino
                """.trimIndent()
            misterio4 = "La Transfiguración"
            meditacionMisterio4 = """
                «Seis días después, Jesús tomó consigo a Pedro, a Santiago y a su hermano Juan, y los llevó aparte, a un monte alto. Y se transfiguró delante de ellos: su rostro se puso brillante como el sol y sus vestidos se volvieron blancos como la luz» (Mt 17, 1-2).
                Jesús tomo a tres de sus discípulos, Pedro, Santiago y Juan. Pedro, quien lo negaría tres veces, pecador arrepentido y perdonado por Cristo, encomendado para recibir la llave de la Iglesia. Pedro quien iría a Roma a testimoniar el amor y las enseñanzas de Jesús.
                
                Reflexión
                
                Santiago quien se traslado a España, seria el primer mártir de los discípulos y fue a su vez a quien la Virgen María le apareciese aun en vida y le animara a construir una Iglesia para su Hijo. Según la tradición, antes de su muerte, Santiago fue a Jerusalén para despedirse de María y del apóstol Juan, allí la Virgen le profetizo su martirio.
                
                El Apóstol  Juan, el discípulo amado de Jesús, fue el único de los apóstoles que permaneció a los pies de la Cruz y por orden de Jesús tomo a María para llevarla a su casa y cuidar de ella. Estos tres apóstoles vieron a Jesús transfigurarse, fueron testigos de la gloria divina del Señor y recibieron la gracia de ver el Cielo. Eran tanta la felicidad que sentían que los tres deseaban quedarse allí.
                
                En este Evangelio Jesus nos enseña que existe un lugar mucho mejor que este lugar donde vivimos; donde muchas veces el sufrimiento nos confunde, las desaveniencias nos llenan de desesperanza y nuestros pobres sentimientos de envidia y egoismo nos hacen debiles a las malas tentaciones. Las tribulaciones y el ruido humano disturba nuestros sentidos. Y muchas veces las necesidades materiales vacian nuestro espiritu y endurecen nuestro corazon.
                
                Asi como los apostoles, Jesus desea que nos apartemos y en el silencio encontremos al Padre porque asi lo hacia El. Nos invita a valorar la oracion y a vivir el mandamiento que nos dejó: “Amense los unos a los otros como Yo los he amado”, para que? Para que cuando nos encontremos cara a cara con el Padre podamos decirle cuanto hemos amado a nuestros hermanos.
                
                Señor, el ruido de este mundo nos atrapa y nos aleja de Ti, deseamos buscarte en el silencio y encontrarnos alli contigo. Sabemos que el camino al Cielo lo vamos construyendo diariamente con nuestras acciones. Te pedimos que podamos transfigurar nuestros sentimientos en los tuyos para amar a nuestros hermanos de la misma manera que tu nos amas.
                """.trimIndent()
            misterio5 = "La Institución de la Eucaristía"
            meditacionMisterio5 = """
                «Mientras estaban comiendo, tomó Jesús pan y lo bendijo, lo partió y, dándoselo a sus discípulos, dijo: "Tomad, comed, éste es mi cuerpo"» (Mt 26, 26).
                Podríamos imaginar a los discipulos alrededor de Jesus, escuchando palabras que quizas no comprendían, puesto que para ellos y para cualquiera de nosotros sería dificil entender que un pedazo de pan y una porción de vino puedan convertirse en el Cuerpo y la Sangre de Cristo. Para esto se requiere de una gracia especial que solamente viene de Dios y que es recibida solo por aquellos de corazon humilde “que creen sin haber visto”. Como diría Jesus a Santo Tomas en el momento de la Resurreccion.
                
                Reflexión
                
                Los misterios luminosos nos alumbran un camino que inicia desde el momento en que somos bautizados, luego nos conduce a entender la debilidad de Jesus ante el amor de su Madre; mostrandonos el poder de intercesión de María como abogada nuestra. Mas adelante nos invita a convertirnos con sinceridad y a creer en el Evangelio para llevarlo precisamente allí donde los sacerdotes no pueden llegar. Pero para esto Jesus nos pide tener un corazon humilde, un espiritu transfigurado en el Suyo y nos llama aparte para imitarlo en la oración. Luego, los apostoles nos ilustran a Jesus en su gloria divina, nos describen un lugar mas bello que este mundo y nos alientan a comprender que el sufrimiento es la esperanza de nuestra salvación. Y en este ultimo misterio Jesus nos revela que se queda con nosotros en el Pan Eucarístico y nos pide “coman y beban de el porque este es mi Cuerpo”. Jesus nos dice claramente “Quien come mi carne y bebe mi sangre tiene vida eterna y yo le resucitaré en el último día”.
                
                En este Evangelio Jesus tambien instituye la Orden Sacerdotal, puesto que pide a sus Apostoles “Hagan esto en memoria mia”, que repitan lo que El hizo en la Ultima Cena. Los Apostoles de Jesus no han dejado de cumplir este mandato, porque sus sucesores han dejado todo para seguirlo y esos son nuestros santos sacerdotes.
                
                Pidamos al Señor que nos regale la virtud de la fe, para que creer firmemente en la Santa Eucaristia, para disponer de un tiempo para acompañar a Jesus en el Sagrario fuente de gracia que sacia nuestro espiritu y lo fortalece. Para asistir a la celebracion de la Santa Eucaristia, la Santa Misa. Para que con nuestras buenas acciones podamos día a día construir el camino de nuestra salvación, para ser dignos de entrar en su Reino y vivir junto a El por toda la eternidad.
                
                Pedimos al Señor santidad para nuestros sacerdotes, fortaleza para todos los seminaristas y religiosos que se encuentran en momentos de dicernimiento. Le pedimos tambien por abundancia de vocaciones en el mundo que puedan apoyar a Jesus en la misión de su Iglesia
                """.trimIndent()
        }
    }

    private fun activaSonido() {
        var cadena: String?
        when (voyMisterio) {
            0 -> {
                cadena =
                    "Por la señal de la Santa Cruz, de nuestros enemigos, líbranos Señor Dios Nuestro."
                cadena += "Señor mío, Jesucristo," +
                        "Dios y Hombre verdadero, Creador, Padre y Redentor mío," +
                        "por ser Vos quien sois y porque os amo sobre todas las cosas," +
                        "me pesa de todo corazón haberos ofendido;" +
                        "propongo firmemente nunca más pecar," +
                        "apartarme de todas las ocasiones de ofenderos," +
                        "confesarme y, cumplir la penitencia que me fuera impuesta." +
                        "Ofrezco, Señor, mi vida, obras y trabajos," +
                        "en satisfacción de todos mis pecados, y, así como lo suplico, así confío en vuestra bondad y misericordia infinita," +
                        "que los perdonareis, por los méritos de vuestra preciosísima sangre, pasión y muerte, y me daréis gracia para enmendarme, y perseverar en vuestro santo amor y servicio," +
                        "hasta el fin de mi vida." + "Amén."
                cadena += "Señor, ábreme los labios.."
                cadena += "Dios mio, ven en mi auxilio.."
                cadena += "Gloria al Padre, y al Hijo y al Espíritu Santo."

                habla(cadena)
                //speaker!!.speak(cadena)
            }

            1 -> {
                cadena = "Primer misterio: $misterio1"
                if (meditados) cadena += meditacionMisterio1
                colaHabla(cadena)
                rellenaInicio()
            }

            2 -> {
                // Segundo misterio
                cadena = "Segundo misterio: $misterio2"
                if (meditados) cadena += meditacionMisterio2
                colaHabla(cadena)
                rellenaFin()
            }

            3 -> {
                // Tercer misterio
                cadena = "Tercer misterio: $misterio3"
                if (meditados) cadena += meditacionMisterio3
                colaHabla(cadena)
                rellenaInicio()
            }

            4 -> {
                // Cuarto misterio
                cadena = "Cuarto misterio: $misterio4"
                if (meditados) cadena += meditacionMisterio4
                colaHabla(cadena)
                rellenaFin()
            }

            5 -> {
                // Quinto misterio
                cadena = "Quinto misterio: $misterio5"
                if (meditados) cadena += meditacionMisterio5
                colaHabla(cadena)
                rellenaInicio()

                cadena += "Letanías de la Santísima Virgen. Señor ten piedad.." +
                        "Cristo, ten piedad.." +
                        "Señor, ten piedad.." +
                        "Cristo, óyenos.." +
                        "Cristo, escúchanos.." +
                        "Dios Padre celestial.." +
                        "Dios Hijo, Redentor del mundo.." +
                        "Dios Espíritu Santo.." +
                        "Trinidad Santa, un solo Dios.." +
                        "Santa María,." +
                        "Santa Madre de Dios.." +
                        "Santa Virgen de las Vírgenes.." +
                        "Madre de Cristo.." +
                        "Madre de la divina gracia.." +
                        "Madre purísima.." +
                        "Madre castísima.." +
                        "Madre inviolada.." +
                        "Madre virgen.." +
                        "Madre inmaculada.." +
                        "Madre amable.." +
                        "Madre del buen consejo.." +
                        "Madre del Creador.." +
                        "Madre del Salvador.." +
                        "Madre de la Iglesia.." +
                        "Virgen prudentísima.." +
                        "Virgen digna de veneración.." +
                        "Virgen digna de alabanza.." +
                        "Virgen poderosa.." +
                        "Virgen clemente.." +
                        "Virgen fiel.." +
                        "Espejo de justicia.." +
                        "Ideal de Santidad.." +
                        "Trono de sabiduría.." +
                        "Causa de nuestra alegría.." +
                        "Templo del Espíritu Santo.." +
                        "Honor de la humanidad.." +
                        "Modelo de entrega a Dios.." +
                        "Rosa escogida.." +
                        "Fuerte como la torre de David.." +
                        "Hermosa como la torre de marfil.." +
                        "Casa de oro.." +
                        "Arca de la Nueva Alianza.." +
                        "Puerta del cielo.." +
                        "Estrella de la mañana.." +
                        "Salud de los enfermos.." +
                        "Refugio de los pecadores.." +
                        "Consoladora de los afligidos.." +
                        "Auxilio de los cristianos.." +
                        "Reina de los Ángeles.." +
                        "Reina de los Patriarcas.." +
                        "Reina de los Profetas.." +
                        "Reina de los Apóstoles.." +
                        "Reina de los Mártires." +
                        "Reina de los Confesores de la fe.." +
                        "Reina de las Vírgenes.." +
                        "Reina de todos los Santos.." +
                        "Reina concebida sin pecado original.." +
                        "Reina asunta a los Cielos.." +
                        "Reina del Santísimo Rosario.." +
                        "Reina de la paz.." +
                        "Reina de las fmailias.. " +
                        "Cordero de Dios, que quitas el pecado del mundo.." +
                        "Cordero de Dios, que quitas el pecado del mundo.." +
                        "Cordero de Dios, que quitas el pecado del mundo..." +
                        "Bajo tu protección los acogemos, Santa Madre de Dios, no deseches las súplics que dirigimos en nuestras necesidades, antes bien, líbranos siempre de todo peligro, oh Virgen gloriosa y bendita." +
                        "Ruega por nosotros, Santa Madre de Dios." +
                        "Para que seamos dignos de alcanzar las promesas de Nuestro Señor Jesucristo. Amén." +
                        "Oremos: Te rogamos Señor y Dios nuestro, nos concedas a tus siervos gozar de perpétua salud de alma y cuerpo, y, por la gloriosa intersección de la Bienaventurada siempre Virgen María ser librados de la tristeza presente y disfrutar de la eterna alegría del Cielo. Por Jesucristo nuestro Señor. Amén" +
                        "Por la persona e intenciones del Romano Pontífice y las necesidades de la Santa Iglesia." + "Te pedimos Señor, nos concedas a nosotros tus siervos, gozar de perpetua salud de alma y cuerpo, y por la gloriosa intercesión de la bienaventurada siempre Virgen María, seamos librados de las tristezas presentes y gocemos de la eterna alegría. Por Jesucristo, nuestro Señor. Amén."
                colaHabla(cadena)
            }
        }
    }

    private fun ponAveMaria(am: Int) {
        binding.numeroAveMaria.text = am.toString()
    }

    fun incrementaAveMaria() {

        avemaria++
        binding.numeroAveMaria.text = avemaria.toString()

        if (avemaria == 10) binding.botonSiguienteAveMaria.isEnabled = false
    }


    private fun rellenaContenido(voy: Int) {
        var cadena: String

        voyMisterio = voy

        binding.scrollRosario.scrollY = 0

        cadena = "Los misterios de hoy son los $tituloMisterios"
        binding.tipoMisterios.text = cadena

        when (voy) {
            0 -> {
                binding.idTituloRosario.text = getString(R.string.inicio)
                binding.idImagen.visibility = View.GONE
                cadena =
                    "Por la señal de la Santa Cruz, de nuestros enemigos líbranos Señor Dios Nuestro.\n\n"
                cadena += """
                Señor mío, Jesucristo,
                Dios y Hombre verdadero, Creador, Padre y Redentor mío,
                por ser Vos quien sois y porque os amo sobre todas las cosas,
                me pesa de todo corazón haberos ofendido;
                propongo firmemente nunca más pecar,
                apartarme de todas las ocasiones de ofenderos,
                confesarme y, cumplir la penitencia que me fuera impuesta.
                
                Ofrezco, Señor, mi vida, obras y trabajos,
                en satisfacción de todos mis pecados, y, así como lo suplico, así confío en vuestra bondad y misericordia infinita,
                que los perdonareis, por los méritos de vuestra preciosísima sangre, pasión y muerte, y me daréis gracia para enmendarme, y perseverar en vuestro santo amor y servicio,
                hasta el fin de mi vida.
                
                Amén.
                
                
                """.trimIndent()
                cadena += "Señor, ábreme los labios R/ y mi boca proclamará tu alabanza\n"
                cadena += "Dios mio, ven en mi auxilio R/ Señor, date prisa en socorrerme\n"
                cadena += "Gloria al Padre, y al Hijo y al Espíritu Santo.\n"

                binding.idMeditacion.text = ""
                binding.idContenidoRosario.text = cadena
                binding.idContenidoRosario2.visibility = View.GONE
            }

            1 -> {
                cadena = "Primer misterio\n$misterio1"
                binding.idImagen.visibility = View.VISIBLE
                binding.idImagen.setImageDrawable(obtenImagen(1, tituloMisterios))
                if (meditados) binding.idMeditacion.text = meditacionMisterio1
                binding.idTituloRosario.text = cadena
                cadena = rellenaPNyAM()
                binding.idContenidoRosario.text = cadena
                cadena = """
                    Gloria
                    
                    María, Madre de gracia, Madre de misericordia.
                    en la vida y en la muerte, ampáranos Señor. Amén.
                    
                    
                    """.trimIndent()
                cadena += """
                    ¡Oh Jesús mío!,
                    perdona nuestros pecados,
                    líbranos del fuego del infierno,
                    lleva todas las almas al cielo
                    y socorre especialmente
                    a las más necesitadas de tu Misericordia.
                    """.trimIndent()
                binding.idContenidoRosario2.text = cadena
                binding.idContenidoRosario2.visibility = View.VISIBLE
            }

            2 -> {
                cadena = "Segundo misterio\n$misterio2"
                if (meditados) binding.idMeditacion.text = meditacionMisterio2
                binding.idImagen.setImageDrawable(obtenImagen(2, tituloMisterios))
                binding.idTituloRosario.text = cadena
                cadena = rellenaPNyAM()
                binding.idContenidoRosario.text = cadena
                cadena = """
                    Gloria
                    
                    María, Madre de gracia, Madre de misericordia.
                    en la vida y en la muerte, ampáranos Señor. Amén.
                    
                    
                    """.trimIndent()
                cadena += """
                    ¡Oh Jesús mío!,
                    perdona nuestros pecados,
                    líbranos del fuego del infierno,
                    lleva todas las almas al cielo
                    y socorre especialmente
                    a las más necesitadas de tu Misericordia.
                    """.trimIndent()
                binding.idContenidoRosario2.text = cadena
                binding.idContenidoRosario2.visibility = View.VISIBLE
            }

            3 -> {
                cadena = "Tercer misterio\n$misterio3"
                if (meditados) binding.idMeditacion.text = meditacionMisterio3
                binding.idImagen.setImageDrawable(obtenImagen(3, tituloMisterios))
                binding.idTituloRosario.text = cadena
                cadena = rellenaPNyAM()
                binding.idContenidoRosario.text = cadena
                cadena = """
                    Gloria
                    
                    María, Madre de gracia, Madre de misericordia.
                    en la vida y en la muerte, ampáranos Señor. Amén.
                    
                    
                    """.trimIndent()
                cadena += """
                    ¡Oh Jesús mío!,
                    perdona nuestros pecados,
                    líbranos del fuego del infierno,
                    lleva todas las almas al cielo
                    y socorre especialmente
                    a las más necesitadas de tu Misericordia.
                    """.trimIndent()
                binding.idContenidoRosario2.text = cadena
                binding.idContenidoRosario2.visibility = View.VISIBLE
            }

            4 -> {
                if (meditados) binding.idMeditacion.text = meditacionMisterio4
                binding.idImagen.setImageDrawable(obtenImagen(4, tituloMisterios))
                cadena = "Cuarto misterio\n$misterio4"
                binding.idTituloRosario.text = cadena
                cadena = rellenaPNyAM()
                binding.idContenidoRosario.text = cadena
                cadena = """
                    Gloria
                    
                    María, Madre de gracia, Madre de misericordia.
                    en la vida y en la muerte, ampáranos Señor. Amén.
                    
                    
                    """.trimIndent()
                cadena += """
                    ¡Oh Jesús mío!,
                    perdona nuestros pecados,
                    líbranos del fuego del infierno,
                    lleva todas las almas al cielo
                    y socorre especialmente
                    a las más necesitadas de tu Misericordia.
                    """.trimIndent()
                binding.idContenidoRosario2.text = cadena
                binding.idContenidoRosario2.visibility = View.VISIBLE
            }

            5 -> {
                if (meditados) binding.idMeditacion.text = meditacionMisterio5
                binding.idImagen.setImageDrawable(obtenImagen(5, tituloMisterios))
                cadena += "Quinto misterio\n$misterio5"
                binding.idTituloRosario.text = cadena
                cadena = rellenaPNyAM()
                binding.idContenidoRosario.text = cadena
                cadena = """
                    Gloria
                    
                    María, Madre de gracia, Madre de misericordia.
                    en la vida y en la muerte, ampáranos Señor. Amén.
                    
                    
                    """.trimIndent()

                cadena += """
                    ¡Oh Jesús mío!,
                    perdona nuestros pecados,
                    líbranos del fuego del infierno,
                    lleva todas las almas al cielo
                    y socorre especialmente
                    a las más necesitadas de tu Misericordia.
                    """.trimIndent()

                cadena += """
                    Letanías de la Santísima Virgen:
                    
                    Señor, ten piedad R/Señor, ten piedad
                    Cristo, ten piedad R/ Cristo, ten piedad
                    Señor, ten piedad R/ Señor, ten piedad
                    Cristo, óyenos R/ Cristo, óyenos
                    Cristo, escúchanos R/ Cristo, escúchanos
                    
                    Dios Padre celestial. R/ Ten misericordia de nosotros
                    Dios Hijo, Redentor del mundo. R/ Ten misericordia de nosotros
                    Dios Espíritu Santo. R/ Ten misericordia de nosotros
                    Trinidad Santa, un solo Dios. R/ Ten misericordia de nosotros
                    
                    Santa María, R/ Ruega por nosotros
                    Santa Madre de Dios,
                    Santa Virgen de las Vírgenes,
                    Madre de Cristo,
                    Madre de la divina gracia,
                    Madre purísima,
                    Madre castísima,
                    Madre inviolada, 
                    Madre virgen,
                    Madre inmaculada,
                    Madre amable,
                    Madre admirable,
                    Madre del buen consejo,
                    Madre del Creador,
                    Madre del Salvador,
                    Virgen de la Iglesia,
                    Virgen prudentísima,
                    Virgen digna de veneración,
                    Virgen digna de alabanza
                    Virgen poderosa,
                    Virgen clemente,
                    Virgen fiel,
                    Espejo de justicia,
                    Ideal de santidad,
                    Trono de sabiduría,
                    Causa de nuestra alegría,
                    Templo del Espíritu Santo
                    Honor de la humanidad,
                    Modelo de entrega a Dios,
                    Rosa escogida,
                    Fuerte como la torre de David,
                    Hermosa como torre de marfil,
                    Casa de oro,
                    Arca de la Nueva Alianza,
                    Puerta del cielo,
                    Estrella de la mañana,
                    Salud de los enfermos,
                    Refugio de los pecadores,
                    Consoladora de los afligidos,
                    Auxilio de los cristianos,
                    Reina de los Ángeles,
                    Reina de los Patriarcas,
                    Reina de los Profetas,
                    Reina de los Apóstoles,
                    Reina de los Mártires,
                    Reina de los Confesores de la fe,
                    Reina de las Vírgenes,
                    Reina de todos los Santos,
                    Reina concebida sin pecado original,
                    Reina asunta a los Cielos,
                    Reina del Santísimo Rosario,
                    Reina de la paz.
                    Reina de las familias.
                    
                    Cordero de Dios, que quitas el pecado del mundo. R/ Perdónanos Señor
                    Cordero de Dios, que quitas el pecado del mundo. R/ Escúchanos Señor
                    Cordero de Dios, que quitas el pecado del mundo. R/ Ten misericordia de nosotros
                    
                    Bajo tu protección nos acogemos, Santa Madre de Dios, no deseches las úplicas que te dirigimos en nuestras necesidades, antes bien, líbranos siempre de todo peligro, oh Virgen gloriosa y bendita.
                    Ruega por nosotros, Santa Madre de Dios.
                    Para que seamos dignos de alcanzar las promesas de Nuestro Señor Jesucristo. Amén.
                    
                    Oremos: Te rogamo, Señor y Dios nuestro, nos concedas a tus siervos gozar de perpetua salud de alma y cuerpo, y, por la gloriosa intercesión de la Bienaventurada siempre Virgen María, ser librados de la tristeza presente y disfrutar de la eterna alegría del Cielo. Por Jesucristo Nuestro Señor. Amén.
                    Por la persona e intenciones del Romano Pontífice y las necesidades de la Santa Igleisa.
                    Padrenuestro, Avemaría y Gloria al Padre
                    Ave María Purísima R/ Sin pecado concebida
                    """.trimIndent()
                binding.idContenidoRosario2.text = cadena
                binding.idContenidoRosario2.visibility = View.VISIBLE
            }
        }
    }

    private fun obtenImagen(misterio: Int, titulo: String?): Drawable? {
        var imagen: Drawable? = AppCompatResources.getDrawable(this, R.drawable.gozoso1) //resources.getDrawable(R.drawable.gozoso1)
        when (titulo) {
            "gozosos" -> when (misterio) {
                1 -> imagen = AppCompatResources.getDrawable(this, R.drawable.gozoso1)
                2 -> imagen = AppCompatResources.getDrawable(this, R.drawable.gozoso2)
                3 -> imagen = AppCompatResources.getDrawable(this, R.drawable.gozoso3)
                4 -> imagen = AppCompatResources.getDrawable(this, R.drawable.gozoso4)
                5 -> imagen = AppCompatResources.getDrawable(this, R.drawable.gozoso5)
            }

            "dolorosos" -> when (misterio) {
                1 -> imagen = AppCompatResources.getDrawable(this, R.drawable.doloroso1)
                2 -> imagen = AppCompatResources.getDrawable(this, R.drawable.doloroso2)
                3 -> imagen = AppCompatResources.getDrawable(this, R.drawable.doloroso3)
                4 -> imagen = AppCompatResources.getDrawable(this, R.drawable.doloroso4)
                5 -> imagen = AppCompatResources.getDrawable(this, R.drawable.doloroso5)
            }

            "gloriosos" -> when (misterio) {
                1 -> imagen = AppCompatResources.getDrawable(this, R.drawable.glorioso1)
                2 -> imagen = AppCompatResources.getDrawable(this, R.drawable.glorioso2)
                3 -> imagen = AppCompatResources.getDrawable(this, R.drawable.glorioso3)
                4 -> imagen = AppCompatResources.getDrawable(this, R.drawable.glorioso4)
                5 -> imagen = AppCompatResources.getDrawable(this, R.drawable.glorioso5)
            }

            "luminosos" -> when (misterio) {
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
        var cadena = """
            Padrenuestro:
            
            Padre nuestro que estás en el cielo, santificado sea tu Nombre; venga a nosotros tu Reino; hágase tu voluntad en la tierra como en el cielo. Danos hoy nuestro pan de cada día; perdona nuestras ofensas,como también nosotros perdonamos a los que nos ofenden; no nos dejes caer en la tentación, y líbranos del mal. Amén.
            
            
            """.trimIndent()
        cadena += "Avemaría:\nDios te salve María, llena eres de gracia, el Señor es contigo. Bendita Tú eres entre todas las mujeres y bendito es el fruto de tu vientre Jesús. "
        cadena += "Santa María, Madre de Dios, ruega por nosotros pecadores, ahora y en la hora de nuestra muerte. Amén"

        return cadena
    }

    private fun rellenaInicio() {
        colaHabla("Padre nuestro que estás en el cielo, santificado sea tu nombre, venga a nosotros tu reino, hágase tu voluntad así en la tierra como en el cielo")
        silencio(9000)
        for (i in 0..9) {
            colaHabla("Dios te salve María, llena eres de gracia, el señor es contigo, bendita tu eres entre todas las mujeres, y bendito es el fruto de tu vientre Jesús")
            silencio(7000)
        }
        colaHabla("Gloria al padre al hijo y al espíritu santo")
        silencio(2000)
        colaHabla(
            """María, Madre de gracia, Madre de misericordia en la vida y en la muerte, ampáranos Señor. Amén.
 ¡Oh Jesús mío!,
perdona nuestros pecados, líbranos del fuego del infierno, lleva todas las almas al cielo y socorre especialmente a las más necesitadas de tu Misericordia."""
        )
    }

    private fun rellenaFin() {
        silencio(9000)
        colaHabla("Danos hoy nuestro pan de cada día, perdona nuestras ofensas como también nosotros perdonamos a los que nos ofenden, no nos dejes caer en la tentación y líbranos del mal")
        for (i in 0..9) {
            silencio(7000)
            if (i == 9) colaHabla("Gloria.")
            colaHabla("Santa María madre de Dios, ruega por nosotros pecadores, ahora y en la hora de nuestra muerte amén")
        }
        silencio(2000)
        colaHabla("como era en un principio ahora y siempre por los siglos de los siglos amen. María, Madre de gracia, Madre de misericordia en la vida y en la muerte, ampáranos Señor. Amén.")
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
                        "Necesitarás habilitar manualmente el permiso de notificaciones si quieres recibirlas en su móvil",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun habla(text: String) {
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utteranceId")

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utteranceId")
    }

    private fun colaHabla(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "utteranceId")
    }

    private fun silencio(duracion: Long) {
        textToSpeech.playSilentUtterance(duracion, TextToSpeech.QUEUE_ADD, null)
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
                ) Toast.makeText(this, "Rechazando este permisos no podrá recibir recordatorios del rezo del rosario", Toast.LENGTH_LONG).show()

                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
            }
        }
    }

    private fun alarma() {
        val sharedPreferences = getSharedPreferences("mis_preferencias", Context.MODE_PRIVATE)
        val tareaProgramada = sharedPreferences.getBoolean("tarea_programada", false)

        if (!tareaProgramada) {
            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyAlarmWorker>(1, TimeUnit.MINUTES,
                flexTimeInterval = 1, flexTimeIntervalUnit = TimeUnit.MINUTES)
                .setInitialDelay(1, TimeUnit.MINUTES)

                .build()

            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                    "daily_alarm",
                    ExistingPeriodicWorkPolicy.KEEP,
                    dailyWorkRequest
                )
        }

        val editor = sharedPreferences.edit()
        editor.putBoolean("tarea_programada", true)
        editor.apply()
    }
}