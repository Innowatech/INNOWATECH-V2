package com.example.model

import androidx.annotation.DrawableRes

enum class ModuleCategory(
    val key: String,
    val title: String,
    val shortTitle: String
) {
    ANWENDUNG("Anwendung", "Anwendung", "Anwendung"),
    PARAMETER("Parameter", "Parameter", "Parameter"),
    HILFSMITTEL("Hilfsmittel", "Hilfsmittel", "Hilfsmittel"),
    KATALOG_CHECKLISTEN("Katalog & Checklisten", "Katalog & Checklisten", "Katalog")
}

enum class ModuleId(
    val id: String,
    val title: String,
    val shortSubtitle: String,
    val defaultCategory: ModuleCategory
) {
    STANDARD("standard", "Trinkwasser", "Dosiermenge & Zehrung", ModuleCategory.ANWENDUNG),
    WASSERWERK("wasserwerk", "Wasserwerk", "Förderung, Impuls & 4–20 mA", ModuleCategory.ANWENDUNG),
    SCHWIMMBAD("schwimmbad", "Schwimmbad", "Volumen, Chlor & pH-Korrektur", ModuleCategory.ANWENDUNG),
    PID("pid", "PID Regler", "Sprungversuch & Volumenstrom", ModuleCategory.ANWENDUNG),
    VERDUENNUNG("verdünnung", "Verdünnungsreihe Anolyte", "Hanna 10 ml Küvette", ModuleCategory.ANWENDUNG),
    DOSIERAUSWERTUNG("dosierauswertung", "Dosierauswertung", "Zähler, Quote & Aktivchlor", ModuleCategory.ANWENDUNG),
    SONDEN("sonden", "Sondendaten", "Messbereich & Steilheit RV1", ModuleCategory.PARAMETER),
    ANLAGEN("anlagen", "Produktionsparameter AQD", "47 Aquadron Anlagendaten", ModuleCategory.PARAMETER),
    RADAR("radar", "Vega Radarsensor", "VEGAPULS 11 Inbetriebnahme", ModuleCategory.PARAMETER),
    ENTHAERTER("enthaerter", "Enthärtungsanlagen", "Zykluszeiten & 80-%-Sensor", ModuleCategory.PARAMETER),
    TASCHENLAMPE("taschenlampe", "Taschenlampe", "LED-Blitzlicht & Notlicht", ModuleCategory.HILFSMITTEL),
    SCHLAUCHVOLUMEN("schlauchvolumen", "Schlauchvolumen", "Volumen & Vorlaufzeit", ModuleCategory.HILFSMITTEL),
    AUSLITERN("auslitern", "Ausliter-Hilfe", "Timer & Elektrolyse-Leistung", ModuleCategory.HILFSMITTEL),
    NOTIZEN("notizen", "Service-Notizen", "Bausteine, Diktat & Hotline", ModuleCategory.HILFSMITTEL),
    KUNDENNOTIZEN("kundennotizen", "Kunden- & Eigennotizen", "Archiv, PDF & Kollegen-Mail", ModuleCategory.HILFSMITTEL),
    KOFFER("koffer", "Servicekoffer", "6 L-Boxen · 162 Artikel", ModuleCategory.KATALOG_CHECKLISTEN),
    ENTHAERTERTAUSCH("enthaertertausch", "Checkliste Enthärtertausch", "Aufnahme, Turbinen & Verteiler", ModuleCategory.KATALOG_CHECKLISTEN);

    companion object {
        fun fromId(id: String): ModuleId? = entries.find { it.id == id }
    }
}

data class ModuleConfig(
    val module: ModuleId,
    val visible: Boolean = true,
    val userAllowed: Boolean = true
)

data class SondeInfo(
    val id: String,
    val label: String,
    val messRv1: String,
    val steilRv1: String
)

data class EnthaerterInfo(
    val id: String,
    val label: String,
    val harz: String,
    val kapazitaet: String,
    val sole: String,
    val salz: String,
    val rueckfuelle: String,
    val rueckspuelen: String,
    val besalzen: String,
    val schnellspuelen: String,
    val soleNachf: String
)

data class AnlageInfo(
    val id: String,
    val label: String,
    val durchfluss: String,
    val strom: String,
    val spannung: String,
    val zeit: String,
    val prod: String,
    val pause: String
)

data class KofferItem(
    val nr: String,
    val beschreibung: String,
    val hersteller: String,
    val hstNr: String,
    val inNr: String,
    val soll: String
)

data class KofferBox(
    val id: String,
    val name: String,
    val shortName: String,
    val items: List<KofferItem>,
    @DrawableRes val imageRes: Int
)

data class KofferPin(
    val nr: String,
    val xFraction: Float,
    val yFraction: Float
)

data class VegaTankSpec(
    val volumeLiters: String,
    val label: String,
    val maxMm: Int,
    val diameterMm: Int
)

data class VegaStep(
    val num: String,
    val title: String,
    val summaryBullets: List<String>,
    val note: String? = null,
    @DrawableRes val imageRes: Int? = null
)

data class UserAccount(
    val kuerzel: String,
    val name: String,
    val email: String,
    val pinHash: String,
    val status: String,
    val isAdmin: Boolean,
    val autoUpdate: Boolean,
    val modules: List<String>
)

data class EmailRecipient(
    val name: String,
    val email: String,
    val kuerzel: String = "",
    val isMe: Boolean = false
)

enum class DashboardDensity(val key: String, val label: String, val columns: Int) {
    COMPACT("compact", "Kompakt (3er-Raster)", 3),
    LARGE("large", "Groß (2er-Raster)", 2);

    companion object {
        fun fromKey(key: String?): DashboardDensity =
            if (key == LARGE.key) LARGE else COMPACT
    }
}

data class EnthaerterTypSpec(
    val value: String,
    val displayLabel: String,
    val timeText: String
)

data class EnthaerterChecklistState(
    val recordId: String? = null,
    val kuerzel: String = "",
    val fabnr: String = "",
    val kunde: String = "",
    val ort: String = "",
    val anlage: String = "",
    val typ: String = "",
    val kabinettNeu: String = "",
    val kabinettArt: String = "",
    val schlauchHv: String = "",
    val schlauchHvCustom: String = "",
    val schlauchHvNeu: String = "",
    val schlauchAbfluss: String = "",
    val schlauchAbflussCustom: String = "",
    val panzerGroesse: String = "",
    val panzerGroesseCustom: String = "",
    val panzerLaenge: String = "",
    val panzerLaengeCustom: String = "",
    val panzerAnschluss: String = "",
    val panzerAnschlussCustom: String = "",
    val schwimmerlanze: String = "",
    val turbine: String = "",
    val fotos: String = "",
    val bemerkung: String = ""
)

data class SavedEnthaerterRecord(
    val id: String,
    val title: String,
    val updatedAtIso: String,
    val state: EnthaerterChecklistState
)

data class KundenNotizDraft(
    val id: String? = null,
    val typ: String = "Kundennotiz",
    val sn: String = "",
    val kunde: String = "",
    val betreff: String = "",
    val text: String = ""
)

data class KundenNotizEntry(
    val id: String,
    val typ: String,
    val sn: String,
    val kunde: String,
    val betreff: String,
    val text: String,
    val author: String,
    val createdAtIso: String,
    val updatedAtIso: String
)

data class InnowatechDatabases(
    val sonden: List<SondeInfo> = emptyList(),
    val enthaerter: List<EnthaerterInfo> = emptyList(),
    val anlagen: List<AnlageInfo> = emptyList(),
    val koffer: List<KofferBox> = emptyList()
)

data class CalculatorInputs(
    // Standard (Trinkwasser)
    val stdZiel: String = "",
    val stdVol: String = "",
    val stdAnol: String = "",
    val stdZehrung: String = "",
    // Wasserwerk
    val wwM3Tag: String = "",
    val wwStunden: String = "",
    val wwLsek: String = "",
    val wwM3h: String = "",
    val wwAnolKonz: String = "",
    val wwZiel: String = "",
    val wwZehrung: String = "",
    val wwImpuls: String = "",
    val wwMaxFlow: String = "",
    // Schwimmbad
    val poolLaenge: String = "",
    val poolBreite: String = "",
    val poolTiefe: String = "",
    val poolVolumen: String = "",
    val poolChlorsorte: String = "anolyte",
    val poolChlorIst: String = "",
    val poolChlorSoll: String = "",
    val poolChlorKonz: String = "",
    val poolPhIst: String = "",
    val poolPhSoll: String = "",
    val poolAlk: String = "",
    val poolSaeureKonz: String = "",
    // PID
    val pidBetriebsart: String = "kalt_kombiniert",
    val pidTestL: String = "",
    val pidWirkstoff: String = "",
    val pidChlorDiff: String = "",
    val pidTu: String = "",
    val pidTg: String = "",
    val pidGrundwert: String = "",
    val pidSollwert: String = "",
    // Verdünnung
    val verdAnlagentyp: String = "500",
    val verdCustomKonz: String = "1650",
    val verdCustomZiel: String = "400",
    val verdMesswert: String = "",
    // Dosierauswertung
    val daWzAlt: String = "",
    val daWzNeu: String = "",
    val daDateAlt: String = "",
    val daDateNeu: String = "",
    val daDosAlt: String = "",
    val daDosNeu: String = "",
    val daKonz: String = "",
    val daSoll: String = "",
    // Parameter selections
    val selectedSondeId: String = "",
    val selectedAnlageId: String = "",
    val selectedRadarVol: String = "",
    val radarMinMm: String = "0",
    val radarMaxMm: String = "",
    val selectedEnthaerterId: String = "",
    val enthaerterHaerte: String = "",
    // Hilfsmittel
    val schlauchD: String = "",
    val schlauchL: String = "",
    val schlauchPumpEnabled: Boolean = false,
    val schlauchPumpLh: String = "",
    val ausliterMedium: String = "katholyte",
    val ausliterSec: Int = 60,
    val ausliterKathMl: String = "",
    val ausliterAnolMl: String = ""
)

sealed interface LightboxContent {
    data class VegaGuide(
        val title: String,
        @DrawableRes val imageRes: Int,
        val caption: String
    ) : LightboxContent

    data class KofferPhoto(
        val boxId: String,
        val boxName: String,
        @DrawableRes val imageRes: Int,
        val activePinNr: String,
        val pins: List<KofferPin>,
        val itemSummary: String,
        val fromPicker: Boolean
    ) : LightboxContent

    data class TurbinePhoto(
        val nr: String,
        @DrawableRes val imageRes: Int
    ) : LightboxContent
}
