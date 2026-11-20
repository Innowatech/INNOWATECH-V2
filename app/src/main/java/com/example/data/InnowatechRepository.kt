package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AnlageInfo
import com.example.model.EmailRecipient
import com.example.model.EnthaerterChecklistState
import com.example.model.EnthaerterInfo
import com.example.model.EnthaerterTypSpec
import com.example.model.InnowatechDatabases
import com.example.model.KofferBox
import com.example.model.KofferItem
import com.example.model.KofferPin
import com.example.model.KundenNotizEntry
import com.example.model.ModuleCategory
import com.example.model.ModuleConfig
import com.example.model.ModuleId
import com.example.model.SavedEnthaerterRecord
import com.example.model.SondeInfo
import com.example.model.UserAccount
import com.example.model.VegaStep
import com.example.model.VegaTankSpec
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object InnowatechCalculations {

    data class StandardResult(
        val sollMgL: Double,
        val mengeMl: Double
    )

    fun calcStandard(ziel: Double, volL: Double, anolMgL: Double, zehrung: Double): StandardResult {
        val soll = ziel + zehrung
        val menge = if (anolMgL > 0.0) ((soll * volL) / anolMgL) * 1000.0 else 0.0
        return StandardResult(sollMgL = soll, mengeMl = menge)
    }

    data class WasserwerkResult(
        val konzMgL: Double,
        val bedarfLTag: Double,
        val hubMl: Double,
        val lProM3: Double,
        val pumpe20MaLh: Double
    )

    fun calcWasserwerk(
        m3Tag: Double,
        anolKonz: Double,
        ziel: Double,
        zehrung: Double,
        impulsL: Double,
        maxFlowM3h: Double
    ): WasserwerkResult {
        val konz = ziel + zehrung
        val lProM3 = if (anolKonz > 0.0) (konz * 1000.0) / anolKonz else 0.0
        val bedarf = if (anolKonz > 0.0) (m3Tag * 1000.0 * konz) / anolKonz else 0.0
        val hub = if (anolKonz > 0.0) (impulsL * konz * 1000.0) / anolKonz else 0.0
        val pumpe20 = maxFlowM3h * lProM3
        return WasserwerkResult(konz, bedarf, hub, lProM3, pumpe20)
    }

    data class SchwimmbadResult(
        val chlorText: String,
        val chlorHint: String,
        val chlorError: Boolean,
        val phText: String,
        val phHint: String
    )

    fun calcSchwimmbad(
        volumenL: Double,
        chlorsorte: String,
        chlorIst: Double,
        chlorSoll: Double,
        chlorKonz: Double,
        phIst: Double,
        phSoll: Double,
        alk: Double?,
        saeureKonz: Double?
    ): SchwimmbadResult {
        if (volumenL <= 0.0) {
            return SchwimmbadResult(
                chlorText = "0 l / kg",
                chlorHint = "Fehler: Bitte zuerst das Poolvolumen angeben!",
                chlorError = true,
                phText = "0 l / kg",
                phHint = "Fehler: Bitte zuerst das Poolvolumen angeben!"
            )
        }
        val diff = chlorSoll - chlorIst
        val amt = if (chlorKonz > 0.0 && diff > 0.0) {
            if (chlorsorte == "anolyte") (diff * volumenL) / chlorKonz else (diff * (volumenL / 1000.0)) / chlorKonz
        } else 0.0

        val (chlorText, chlorHint) = if (amt > 0.0) {
            val formatted = if (amt < 1.0 && chlorsorte == "anolyte") {
                "${(amt * 1000.0).roundToInt()} ml"
            } else {
                String.format(Locale.US, "%.2f %s", amt, if (chlorsorte == "anolyte") "l" else "kg")
            }
            "$formatted Zugabe" to "Chlormenge erforderlich ($formatted)"
        } else {
            val txt = if (chlorKonz <= 0.0) "Bitte Wirkstoff eingeben" else "0 l / kg"
            val h = if (chlorKonz <= 0.0) "Bitte Wirkstoffgehalt eingeben." else "Chlorgehalt im Zielbereich oder unvollständig."
            txt to h
        }

        val effAlk = alk?.takeIf { it > 0.0 } ?: 100.0
        val effSk = saeureKonz?.takeIf { it > 0.0 } ?: 20.0
        val pD = phIst - phSoll
        val m3 = volumenL / 1000.0

        val (phText, phHint) = if (phIst > 0.0 && phSoll > 0.0 && pD != 0.0) {
            val ml = abs(pD) * 10.0 * m3 * 15.0 * (if (pD > 0.0) 20.0 / effSk else 1.0) * (effAlk / 100.0)
            val t = if (ml >= 1000.0) {
                String.format(Locale.US, "%.2f l / kg", ml / 1000.0)
            } else {
                String.format(Locale.US, "%.0f ml / g", ml)
            }
            val action = if (pD > 0.0) "pH-Senker" else "pH-Heber"
            t to "$action erforderlich ($t)"
        } else {
            "0 l / kg" to "pH-Wert im Zielbereich oder unvollständig."
        }

        return SchwimmbadResult(
            chlorText = chlorText,
            chlorHint = chlorHint,
            chlorError = false,
            phText = phText,
            phHint = phHint
        )
    }

    data class PidResult(
        val valid: Boolean,
        val pText: String = "-- %",
        val iText: String = "-- s",
        val totbandText: String = "-- mg/l",
        val grunddosierungText: String? = null,
        val durchflussText: String? = null,
        val durchflussLabel: String = "Errechneter Hauptleitungs-Durchfluss",
        val hinweis: String
    )

    fun calcPid(
        betriebsart: String,
        testL: Double?,
        wirkstoff: Double?,
        chlorDiff: Double?,
        tu: Double?,
        tg: Double?,
        grundwert: Double?,
        sollwert: Double?
    ): PidResult {
        val isWarm = betriebsart.contains("warm")
        val isWarmKomb = betriebsart == "warm_kombiniert"
        val missing = buildList {
            if (testL == null) add("Test-Dosiermenge")
            if (wirkstoff == null) add("Tank-Wirkstoffgehalt")
            if (chlorDiff == null) add("Chloranstieg ΔY")
            if (tu == null) add("Totzeit Tu")
            if (tg == null) add("Anstiegszeit Tg")
            if (isWarmKomb && sollwert == null) add("Chlor-Sollwert")
        }
        if (missing.isNotEmpty()) {
            return PidResult(
                valid = false,
                hinweis = "Bitte folgende Werte vollständig eingeben: ${missing.joinToString(", ")}."
            )
        }
        val tL = testL!!
        val anol = wirkstoff!!
        val dC = chlorDiff!!
        val tuVal = tu!!
        val tgVal = tg!!
        val required = listOfNotNull(tL, anol, dC, tuVal, tgVal, if (isWarmKomb) sollwert else null)
        if (required.any { !it.isFinite() || it <= 0.0 }) {
            return PidResult(
                valid = false,
                hinweis = "Fehler: Alle Pflichtwerte müssen gültige Zahlen größer als 0 sein."
            )
        }
        if (isWarm && grundwert != null && (!grundwert.isFinite() || grundwert < 0.0)) {
            return PidResult(
                valid = false,
                hinweis = "Fehler: Der erwartete Restchlorgehalt darf nicht negativ sein."
            )
        }

        val gw = grundwert ?: 0.0
        var p = min(10.0, max(1.0, 0.25 / (dC / tL)))
        if (isWarm) {
            p *= 0.8
            if (gw > 0.0) {
                p *= max(0.6, 1.0 - gw * 0.5)
            }
        }
        val tn = 2.0 * tgVal + tuVal
        val qLh = (tL * anol) / dC
        val qLs = qLh / 3600.0
        val qM3h = qLh / 1000.0

        val baseNote = StringBuilder()
        baseNote.append(
            String.format(
                Locale.US,
                "Praxistest ausgewertet: Totzeit Tu = %.1f s, Anstiegszeit Tg = %.1f s. Nachstellzeit Tn = 2 × Tg + Tu.",
                tuVal,
                tgVal
            )
        )
        if (isWarm) {
            baseNote.append(
                String.format(
                    Locale.US,
                    "\n\nRückrechnung Warmwasser / Zirkulation: Restchlor %.2f mg/l, P-Anteil %.1f %%. Errechneter Umwälz-/Entnahmestrom: %.2f l/s (%.2f m³/h bzw. %.0f l/h).",
                    gw,
                    p,
                    qLs,
                    qM3h,
                    qLh
                )
            )
        } else {
            baseNote.append(
                String.format(
                    Locale.US,
                    "\n\nRückrechnung Hauptleitung (Kaltwasser): Bei %.1f l/h (%.0f mg/l) und +%.2f mg/l Anstieg fließen aktuell ca. %.2f l/s (%.2f m³/h bzw. %.0f l/h Verbrauch).",
                    tL,
                    anol,
                    dC,
                    qLs,
                    qM3h,
                    qLh
                )
            )
        }
        var gdText: String? = null
        if (isWarmKomb && sollwert != null) {
            val gd = max(0.0, sollwert - 0.075)
            gdText = String.format(Locale.US, "%.3f mg/l", gd)
            baseNote.append(
                String.format(
                    Locale.US,
                    "\n\nEmpfohlene Grunddosierung: %.3f mg/l (Erfahrungsabstand 0,075 mg/l zum Sollwert %.3f mg/l).",
                    gd,
                    sollwert
                )
            )
        }

        return PidResult(
            valid = true,
            pText = String.format(Locale.US, "%.1f %%", p),
            iText = String.format(Locale.US, "%.0f s", tn),
            totbandText = "0.02 mg/l",
            grunddosierungText = gdText,
            durchflussText = String.format(Locale.US, "%.2f l/s (%.2f m³/h)", qLs, qM3h),
            durchflussLabel = if (isWarm) "Errechneter Umwälz-/Zirkulationsstrom" else "Errechneter Hauptleitungs-Durchfluss",
            hinweis = baseNote.toString()
        )
    }

    data class VerduennungResult(
        val stufeText: String,
        val analytText: String,
        val wasserText: String,
        val faktorText: String,
        val finalText: String,
        val hinweis: String,
        val isError: Boolean,
        val isWarning: Boolean
    )

    fun calcVerduennung(
        anlagentyp: String,
        customKonz: Double?,
        customZiel: Double?,
        messwert: Double?
    ): VerduennungResult {
        val isCustom = anlagentyp == "custom"
        var teile = 3
        var stufe = "1:3 (Faktor 4)"
        var faktor = 4
        var err = ""
        val maxW = customKonz ?: 0.0
        val ziel = customZiel ?: 400.0

        when (anlagentyp) {
            "250" -> {
                teile = 0
                stufe = "Keine (Direktmessung)"
                faktor = 1
            }
            "500", "600" -> {
                teile = 1
                stufe = "1:1 (Faktor 2)"
                faktor = 2
            }
            "custom" -> {
                if (!maxW.isFinite() || maxW <= 0.0) {
                    err = "Bitte einen gültigen erwarteten Anolyt-Höchstwert größer 0 mg/l eingeben."
                } else if (!ziel.isFinite() || ziel <= 0.0 || ziel >= 500.0) {
                    err = "Der Hanna-Zielwert muss größer 0 und kleiner als 500 mg/l sein."
                } else {
                    faktor = max(1, ceil(maxW / ziel).toInt())
                    teile = faktor - 1
                    stufe = if (teile == 0) "Keine (Direktmessung)" else "1:$teile (Faktor $faktor)"
                }
            }
        }

        if (err.isNotEmpty()) {
            return VerduennungResult(
                stufeText = "--",
                analytText = "--",
                wasserText = "--",
                faktorText = "--",
                finalText = "0.0 mg/l (ppm)",
                hinweis = "Eingabe prüfen: $err",
                isError = true,
                isWarning = false
            )
        }

        val aM = if (teile == 0) 10.0 else 10.0 / faktor
        val wM = 10.0 - aM
        val gem = messwert ?: 0.0
        val finalText = if (gem.isFinite() && gem > 0.0) {
            String.format(Locale.US, "%.1f mg/l (ppm)", gem * faktor)
        } else {
            "0.0 mg/l (ppm)"
        }

        var isWarning = false
        val hinweis = if (isCustom) {
            val erw = maxW / faktor
            val warnText = if (messwert != null && (!messwert.isFinite() || messwert < 0.0 || messwert > 500.0)) {
                isWarning = true
                "\nAchtung: Der eingegebene Hanna-Wert liegt außerhalb des gültigen Messbereichs (0–500 mg/l)."
            } else ""
            val zielLine = if (erw <= ziel) {
                String.format(
                    Locale.US,
                    "Erwarteter Hanna-Wert ca. %.1f mg/l (Ziel %.0f mg/l, Abstand zur Obergrenze %.1f mg/l).",
                    erw,
                    ziel,
                    500.0 - erw
                )
            } else {
                "Zielwert nicht erreicht."
            }
            "Automatische Empfehlung: $stufe.\n$zielLine\nRechnerische Geräte-Abdeckung bis ${500 * faktor} mg/l Anolyt.$warnText"
        } else {
            if (teile == 0) {
                "Direktmessung aktiv: Keine Verdünnung nötig."
            } else {
                "Sicherer Bereich: Stufe $stufe deckt rechnerisch bis zu ${500 * faktor} mg/l ab."
            }
        }

        return VerduennungResult(
            stufeText = stufe,
            analytText = String.format(Locale.US, "%.2f ml / g", aM),
            wasserText = String.format(Locale.US, "%.2f ml / g", wM),
            faktorText = "× $faktor",
            finalText = finalText,
            hinweis = hinweis,
            isError = false,
            isWarning = isWarning
        )
    }

    enum class DosierStatus { NEUTRAL, OK, WARN, ERROR }

    data class DosierauswertungResult(
        val wasserText: String = "-",
        val anolytText: String = "-",
        val quoteText: String = "-",
        val chlorText: String = "-",
        val faktorText: String = "-",
        val tageText: String? = null,
        val wasserTagText: String? = null,
        val dosierTagText: String? = null,
        val hinweis: String,
        val status: DosierStatus = DosierStatus.NEUTRAL
    )

    fun calcDosierauswertung(
        wzAlt: Double?,
        wzNeu: Double?,
        dosAlt: Double?,
        dosNeu: Double?,
        konz: Double?,
        soll: Double?,
        dateAltIso: String,
        dateNeuIso: String
    ): DosierauswertungResult {
        if (wzAlt == null || wzNeu == null || dosAlt == null || dosNeu == null) {
            return DosierauswertungResult(
                hinweis = "Wasserzähler alt/neu und Dosiermenge alt/neu eingeben, um reale Dosierquote und Wirkstoffeintrag zu berechnen."
            )
        }
        val waterM3 = wzNeu - wzAlt
        val waterL = waterM3 * 1000.0
        val dosL = dosNeu - dosAlt

        if (!waterM3.isFinite() || waterM3 <= 0.0) {
            return DosierauswertungResult(
                wasserText = if (waterM3.isFinite()) String.format(Locale.US, "%.2f m³", waterM3) else "-",
                hinweis = "Eingabe prüfen: Wasserzähler neu muss größer als Wasserzähler alt sein.",
                status = DosierStatus.ERROR
            )
        }
        if (!dosL.isFinite() || dosL < 0.0) {
            return DosierauswertungResult(
                wasserText = String.format(Locale.US, "%.2f m³", waterM3),
                anolytText = if (dosL.isFinite()) String.format(Locale.US, "%.2f l", dosL) else "-",
                hinweis = String.format(
                    Locale.US,
                    "Eingabe prüfen: Dosiermenge neu muss größer oder gleich Dosiermenge alt sein (Differenz: %.2f l).",
                    dosL
                ),
                status = DosierStatus.ERROR
            )
        }

        val quotePct = (dosL / waterL) * 100.0
        var days = 0L
        var dateErr = ""
        if (dateAltIso.isNotBlank() && dateNeuIso.isNotBlank()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val d1 = sdf.parse(dateAltIso)
                val d2 = sdf.parse(dateNeuIso)
                if (d1 != null && d2 != null) {
                    days = ((d2.time - d1.time) / 86_400_000.0).roundToLong()
                    if (days <= 0L) dateErr = "Datum neu muss nach Datum alt liegen."
                } else {
                    dateErr = "Ungültiges Datumsformat."
                }
            } catch (_: Exception) {
                dateErr = "Ungültiges Datumsformat."
            }
        }

        val tageText = if (days > 0) "$days ${if (days == 1L) "Tag" else "Tage"}" else null
        val wasserTagText = if (days > 0) String.format(Locale.US, "%.2f m³/Tag", waterM3 / days) else null
        val dosierTagText = if (days > 0) String.format(Locale.US, "%.2f l/Tag", dosL / days) else null

        if (konz != null && konz.isFinite() && konz > 0.0) {
            val clReal = (dosL * konz) / waterL
            val chlorText = String.format(Locale.US, "%.3f mg/l", clReal)
            if (soll != null && soll.isFinite() && soll > 0.0) {
                val faktor = clReal / soll
                val pct = faktor * 100.0
                val faktorText = String.format(Locale.US, "%.0f %%", pct)
                var status = DosierStatus.OK
                var statusMsg = String.format(
                    Locale.US,
                    "Plausibel: Real eindosiertes Aktivchlor liegt bei %.3f mg/l (%.0f %% vom Sollwert %.2f mg/l).",
                    clReal,
                    pct,
                    soll
                )
                if (faktor < 0.7) {
                    status = DosierStatus.WARN
                    statusMsg = String.format(
                        Locale.US,
                        "Hinweis (Unterdosierung): Nur %.0f %% des Sollwerts (%.3f vs. %.2f mg/l). Pumpenhub, Saugleitung oder Chlorgehalt Anolytkonzentrat prüfen.",
                        pct,
                        clReal,
                        soll
                    )
                } else if (faktor > 1.5) {
                    status = DosierStatus.ERROR
                    statusMsg = String.format(
                        Locale.US,
                        "Auffällig (Überdosierung / hohe Zehrung): %.0f %% des Sollwerts (%.3f vs. %.2f mg/l) eindosiert.",
                        pct,
                        clReal,
                        soll
                    )
                }
                if (dateErr.isNotEmpty()) {
                    statusMsg += "\nDatum: $dateErr"
                } else if (days > 0) {
                    statusMsg += String.format(
                        Locale.US,
                        "\nZeitraum %d Tage: %.2f m³/Tag Wasser · %.2f l/Tag Anolyte.",
                        days,
                        waterM3 / days,
                        dosL / days
                    )
                }
                return DosierauswertungResult(
                    wasserText = String.format(Locale.US, "%.2f m³", waterM3),
                    anolytText = String.format(Locale.US, "%.2f l", dosL),
                    quoteText = String.format(Locale.US, "%.2f %%", quotePct),
                    chlorText = chlorText,
                    faktorText = faktorText,
                    tageText = tageText,
                    wasserTagText = wasserTagText,
                    dosierTagText = dosierTagText,
                    hinweis = statusMsg,
                    status = status
                )
            } else {
                val extra = if (dateErr.isNotEmpty()) " ($dateErr)" else if (days > 0) {
                    String.format(Locale.US, " Ø %.2f m³/Tag Wasser & %.2f l/Tag Anolyte.", waterM3 / days, dosL / days)
                } else ""
                return DosierauswertungResult(
                    wasserText = String.format(Locale.US, "%.2f m³", waterM3),
                    anolytText = String.format(Locale.US, "%.2f l", dosL),
                    quoteText = String.format(Locale.US, "%.2f %%", quotePct),
                    chlorText = chlorText,
                    faktorText = "Sollwert eingeben",
                    tageText = tageText,
                    wasserTagText = wasserTagText,
                    dosierTagText = dosierTagText,
                    hinweis = "Real eindosiertes Aktivchlor: $chlorText.$extra",
                    status = DosierStatus.NEUTRAL
                )
            }
        } else {
            val extra = if (dateErr.isNotEmpty()) " ($dateErr)" else ""
            return DosierauswertungResult(
                wasserText = String.format(Locale.US, "%.2f m³", waterM3),
                anolytText = String.format(Locale.US, "%.2f l", dosL),
                quoteText = String.format(Locale.US, "%.2f %%", quotePct),
                chlorText = "Konz. eingeben",
                faktorText = "-",
                tageText = tageText,
                wasserTagText = wasserTagText,
                dosierTagText = dosierTagText,
                hinweis = String.format(
                    Locale.US,
                    "Dosierquote: %.2f %%. Chlorgehalt Anolytkonzentrat eingeben für Aktivchlor-Vergleich.%s",
                    quotePct,
                    extra
                ),
                status = DosierStatus.NEUTRAL
            )
        }
    }

    data class SchlauchResult(
        val proMeterText: String,
        val gesamtText: String,
        val zeitText: String,
        val hint: String
    )

    fun calcSchlauch(dMm: Double, lM: Double, pumpEnabled: Boolean, pumpLh: Double): SchlauchResult {
        val mlM = PI * (dMm / 2.0).pow(2)
        val lPerM = mlM / 1000.0
        val gesL = (mlM * lM) / 1000.0
        if (!pumpEnabled) {
            return SchlauchResult(
                proMeterText = String.format(Locale.US, "%.2f l/m", lPerM),
                gesamtText = String.format(Locale.US, "%.2f l", gesL),
                zeitText = "--",
                hint = ""
            )
        }
        return if (gesL > 0.0 && pumpLh > 0.0) {
            val totMin = (gesL / pumpLh) * 60.0
            val h = floor(totMin / 60.0).toInt()
            val m = floor(totMin % 60.0).toInt()
            val s = ((totMin * 60.0) % 60.0).roundToInt()
            val tStr = if (h > 0) "${h} Std ${m} Min" else "${m} Min ${s.toString().padStart(2, '0')} Sek"
            SchlauchResult(
                proMeterText = String.format(Locale.US, "%.2f l/m", lPerM),
                gesamtText = String.format(Locale.US, "%.2f l", gesL),
                zeitText = tStr,
                hint = String.format(
                    Locale.US,
                    "Bei %.1f l/h benötigt das Produkt ca. %s bis zum Impfpunkt (mind. %.2f l vorpumpen).",
                    pumpLh,
                    tStr,
                    gesL
                )
            )
        } else {
            SchlauchResult(
                proMeterText = String.format(Locale.US, "%.2f l/m", lPerM),
                gesamtText = String.format(Locale.US, "%.2f l", gesL),
                zeitText = "--",
                hint = "Bitte Schlauchmaße und Förderleistung (l/h) der Pumpe eingeben."
            )
        }
    }

    data class Enthaerter80Result(
        val kap80Text: String,
        val literText: String,
        val hint: String
    )

    fun calcEnthaerter80(info: EnthaerterInfo?, haerteDh: Double): Enthaerter80Result {
        if (info == null) {
            return Enthaerter80Result(
                kap80Text = "-",
                literText = "-",
                hint = "Modell und Rohwasserhärte eingeben, um die enthärtete Wassermenge bis zur Regeneration zu berechnen."
            )
        }
        val kapRaw = info.kapazitaet.replace(",", ".")
            .takeWhile { it.isDigit() || it == '.' || it == ' ' }
            .trim()
            .toDoubleOrNull() ?: 0.0
        val kap80 = kapRaw * 0.8
        val kap80Text = String.format(Locale.US, "%.1f °dH·m³", kap80)
        if (haerteDh <= 0.0) {
            return Enthaerter80Result(
                kap80Text = kap80Text,
                literText = "-",
                hint = "Bitte Rohwasserhärte (°dH) eingeben, um die enthärtete Wassermenge bis zur Regeneration zu ermitteln."
            )
        }
        val liter = (kap80 / haerteDh) * 1000.0
        val literRounded = liter.roundToInt()
        val m3 = liter / 1000.0
        val literText = String.format(Locale.US, "%d l (%.2f m³)", literRounded, m3)
        val hint = String.format(
            Locale.US,
            "Bei %.1f °dH und 80 %% Nutzkapazität (Quellharz-Sensor) stehen bis zur Regeneration ca. %d l (%.2f m³) Weichwasser zur Verfügung.",
            haerteDh,
            literRounded,
            m3
        )
        return Enthaerter80Result(kap80Text, literText, hint)
    }
}

object InnowatechRepository {
    const val HOTLINE_EMAIL = "service-hotline@innowatech.de"
    const val APP_VERSION = "v5.6"

    val defaultCategoryOrder = listOf(
        ModuleCategory.ANWENDUNG,
        ModuleCategory.PARAMETER,
        ModuleCategory.HILFSMITTEL,
        ModuleCategory.KATALOG_CHECKLISTEN
    )

    val defaultModules: List<ModuleConfig> = ModuleId.entries.map {
        ModuleConfig(module = it, visible = true, userAllowed = true)
    }

    val radarTankSpecs: List<VegaTankSpec> = listOf(
        VegaTankSpec("40", "40 Liter", 385, 414),
        VegaTankSpec("60", "60 Liter", 535, 414),
        VegaTankSpec("100", "100 Liter", 723, 450),
        VegaTankSpec("200", "200 Liter", 830, 588),
        VegaTankSpec("500", "500 Liter", 1108, 764),
        VegaTankSpec("1000", "1000 Liter", 1190, 1084)
    )

    val vegaSteps: List<VegaStep> = listOf(
        VegaStep(
            num = "0",
            title = "Service-Login (Vorbereitung in VEGA Tools App)",
            summaryBullets = listOf(
                "In der VEGA Tools App oben rechts das Zahnrad-Symbol anwählen und auf Service-Login (Inaktiv) tippen.",
                "Passwort: sw (Abkürzung: Schwarzwald)"
            ),
            note = "Erst nach dem Service-Login wird später das Menü Spezialparameter sichtbar!",
            imageRes = null
        ),
        VegaStep(
            num = "1",
            title = "Messstellenname",
            summaryBullets = listOf(
                "Generiert sich aus: Letzte 3 Ziffern der Fabrikationsnummer – Aquadron-Anlagentyp – Tankvolumen – Tanknummer.",
                "Beispiel: 517-FXM-60-1"
            ),
            note = "Tanknummer ab 1 hochzählend, falls mehrere Tanks beim Kunden vorhanden sind.",
            imageRes = R.drawable.vega_page_1
        ),
        VegaStep(
            num = "2",
            title = "Anwendung",
            summaryBullets = listOf(
                "Mediumtyp: Flüssigkeit",
                "Anwendung: Lagertank"
            ),
            note = "Hinweis: Nach Anpassungen in den Spezialparametern wird dieser Bereich ausgegraut und gesperrt.",
            imageRes = R.drawable.vega_page_2
        ),
        VegaStep(
            num = "3",
            title = "Einheiten",
            summaryBullets = listOf(
                "Distanzeinheit des Gerätes: mm (Millimeter)",
                "Temperatureinheit des Gerätes: °C (Grad Celsius)"
            ),
            imageRes = R.drawable.vega_page_3
        ),
        VegaStep(
            num = "4",
            title = "Abgleich (0% / 100%)",
            summaryBullets = listOf(
                "40 l (Ø 414 mm): Min 0 mm | Max 385 mm",
                "60 l (Ø 414 mm): Min 0 mm | Max 535 mm",
                "100 l (Ø 450 mm): Min 0 mm | Max 723 mm",
                "200 l (Ø 588 mm): Min 0 mm | Max 830 mm",
                "500 l (Ø 764 mm): Min 0 mm | Max 1108 mm",
                "1000 l (Ø 1084 mm): Min 0 mm | Max 1190 mm"
            ),
            note = "Immer ab Sensorunterseite gemessen. Fertigungsbedingte Abweichungen prüfen.",
            imageRes = R.drawable.vega_page_4
        ),
        VegaStep(
            num = "5",
            title = "Skalierung",
            summaryBullets = listOf(
                "Skalierungsgröße: dimensionslos",
                "Skalierungseinheit: - (Strich)",
                "Skalierungsformat: #",
                "100% entspricht: 100",
                "0% entspricht: 0"
            ),
            imageRes = R.drawable.vega_page_5
        ),
        VegaStep(
            num = "6",
            title = "Stromausgang",
            summaryBullets = listOf(
                "Ausgangskennlinie: 0 ... 100% entspricht 4 ... 20 mA",
                "Strombereich: 4 ... 20 mA",
                "Verhalten bei Störung: ≤ 3,6 mA"
            ),
            note = "Wago-Analogeingangskarte wertet Werte < 4 mA und > 20 mA als Fehler.",
            imageRes = R.drawable.vega_page_6
        ),
        VegaStep(
            num = "7",
            title = "Störsignalausblendung",
            summaryBullets = listOf(
                "Aktuelle Messkurve prüfen: Die erste/höchste Welle ist der Wasserspiegel.",
                "Auf Störsignalausblendung unten im Dropdown 'Alles löschen' wählen und Ausführen.",
                "Auf Störsignale bearbeiten alle Einträge in der Tabelle löschen."
            ),
            note = "Nahbereichs-Ausblendung erfolgt über die Spezialparameter!",
            imageRes = R.drawable.vega_page_7
        ),
        VegaStep(
            num = "8",
            title = "Spezialparameter (Service-Login 'sw')",
            summaryBullets = listOf(
                "(1) Begrenzung Messbereichsbeginn aktiv: Haken setzen",
                "(2) Begrenzung Messbereichsbeginn: 30 mm",
                "(3) Faktor für Rauschmittelung steigend: 2",
                "(4) Faktor für Rauschmittelung fallend: 6",
                "(5) „Erstes großes Echo“ aktiv: Haken setzen",
                "(6) Amplitudenschwelle „Erstes großes Echo“: 12 dB"
            ),
            imageRes = R.drawable.vega_page_8
        ),
        VegaStep(
            num = "9",
            title = "Zugriffsschutz (Bluetooth-PIN)",
            summaryBullets = listOf(
                "Menü Erweiterte Funktionen → Zugriffsschutz",
                "Bluetooth-Zugangscode auf 072186 einstellen",
                "Schutz der Parametrierung: Deaktiviert"
            ),
            imageRes = R.drawable.vega_page_9
        )
    )

    val kofferPins: Map<String, List<KofferPin>> = mapOf(
        "dosierung" to listOf(
            KofferPin("1", 0.643f, 0.047f), KofferPin("2", 0.083f, 0.197f),
            KofferPin("3", 0.163f, 0.196f), KofferPin("4", 0.607f, 0.283f),
            KofferPin("5", 0.378f, 0.456f), KofferPin("6", 0.750f, 0.463f),
            KofferPin("7", 0.872f, 0.422f), KofferPin("8", 0.187f, 0.674f),
            KofferPin("9", 0.693f, 0.642f), KofferPin("10", 0.029f, 0.712f),
            KofferPin("11", 0.207f, 0.815f), KofferPin("12", 0.027f, 0.974f),
            KofferPin("13", 0.206f, 0.916f), KofferPin("14", 0.588f, 0.844f)
        ),
        "elektrik" to listOf(
            KofferPin("1", 0.107f, 0.179f), KofferPin("2", 0.283f, 0.215f),
            KofferPin("3", 0.627f, 0.110f), KofferPin("4", 0.418f, 0.205f),
            KofferPin("5", 0.878f, 0.234f), KofferPin("6", 0.100f, 0.483f),
            KofferPin("7", 0.356f, 0.500f), KofferPin("8", 0.326f, 0.664f),
            KofferPin("9", 0.478f, 0.672f), KofferPin("10", 0.644f, 0.653f),
            KofferPin("11", 0.571f, 0.479f), KofferPin("12", 0.781f, 0.425f),
            KofferPin("13", 0.894f, 0.494f), KofferPin("14", 0.067f, 0.960f),
            KofferPin("15", 0.179f, 0.961f), KofferPin("16", 0.295f, 0.963f),
            KofferPin("17", 0.407f, 0.963f), KofferPin("18", 0.519f, 0.961f),
            KofferPin("19", 0.763f, 0.725f), KofferPin("20", 0.801f, 0.925f)
        ),
        "elektrolyse" to listOf(
            KofferPin("1", 0.260f, 0.141f), KofferPin("2", 0.684f, 0.043f),
            KofferPin("3", 0.870f, 0.046f), KofferPin("4", 0.647f, 0.156f),
            KofferPin("5", 0.763f, 0.128f), KofferPin("6", 0.931f, 0.128f),
            KofferPin("7", 0.722f, 0.208f), KofferPin("8", 0.800f, 0.213f),
            KofferPin("9", 0.929f, 0.257f), KofferPin("10", 0.653f, 0.369f),
            KofferPin("11", 0.742f, 0.327f), KofferPin("12", 0.843f, 0.313f),
            KofferPin("13", 0.651f, 0.462f), KofferPin("14", 0.744f, 0.451f),
            KofferPin("15", 0.836f, 0.451f), KofferPin("16", 0.962f, 0.505f),
            KofferPin("17", 0.651f, 0.655f), KofferPin("18", 0.730f, 0.664f),
            KofferPin("19", 0.808f, 0.662f), KofferPin("20", 0.667f, 0.783f),
            KofferPin("21", 0.813f, 0.778f), KofferPin("22", 0.570f, 0.949f),
            KofferPin("23", 0.651f, 0.951f), KofferPin("24", 0.730f, 0.952f)
        ),
        "enthaerter" to listOf(
            KofferPin("1", 0.185f, 0.057f), KofferPin("2", 0.504f, 0.059f),
            KofferPin("3", 0.911f, 0.071f), KofferPin("4", 0.902f, 0.285f),
            KofferPin("5", 0.094f, 0.947f), KofferPin("6", 0.285f, 0.228f),
            KofferPin("7", 0.254f, 0.946f), KofferPin("8", 0.452f, 0.249f),
            KofferPin("9", 0.437f, 0.946f), KofferPin("10", 0.606f, 0.262f),
            KofferPin("11", 0.657f, 0.500f), KofferPin("12", 0.561f, 0.946f),
            KofferPin("13", 0.671f, 0.946f), KofferPin("14", 0.783f, 0.946f),
            KofferPin("15", 0.949f, 0.886f), KofferPin("16", 0.953f, 0.636f),
            KofferPin("17", 0.954f, 0.487f), KofferPin("18", 0.816f, 0.474f),
            KofferPin("19", 0.912f, 0.381f)
        ),
        "netzteile" to listOf(
            KofferPin("1", 0.174f, 0.044f), KofferPin("2", 0.337f, 0.044f),
            KofferPin("3", 0.132f, 0.565f), KofferPin("4", 0.165f, 0.677f),
            KofferPin("5", 0.321f, 0.705f), KofferPin("6a", 0.894f, 0.049f),
            KofferPin("6b", 0.894f, 0.152f), KofferPin("7", 0.718f, 0.245f),
            KofferPin("8", 0.853f, 0.359f), KofferPin("9", 0.510f, 0.471f),
            KofferPin("10", 0.909f, 0.502f), KofferPin("11", 0.519f, 0.649f),
            KofferPin("12", 0.515f, 0.731f), KofferPin("13", 0.519f, 0.814f),
            KofferPin("14", 0.519f, 0.860f), KofferPin("15", 0.519f, 0.908f),
            KofferPin("16", 0.105f, 0.850f)
        ),
        "ventile" to listOf(
            KofferPin("1", 0.176f, 0.057f), KofferPin("2", 0.390f, 0.057f),
            KofferPin("3", 0.610f, 0.057f), KofferPin("4", 0.782f, 0.057f),
            KofferPin("5", 0.897f, 0.057f), KofferPin("6", 0.058f, 0.372f),
            KofferPin("7", 0.060f, 0.538f), KofferPin("8", 0.270f, 0.431f),
            KofferPin("9", 0.617f, 0.288f), KofferPin("10", 0.590f, 0.501f),
            KofferPin("11", 0.870f, 0.305f), KofferPin("12", 0.072f, 0.791f),
            KofferPin("13", 0.430f, 0.938f), KofferPin("14", 0.575f, 0.679f),
            KofferPin("15", 0.573f, 0.930f), KofferPin("16", 0.690f, 0.974f),
            KofferPin("17", 0.691f, 0.658f), KofferPin("18", 0.791f, 0.688f),
            KofferPin("19", 0.969f, 0.717f), KofferPin("20", 0.867f, 0.978f),
            KofferPin("21", 0.947f, 0.976f)
        )
    )

    val enthaerterTypSpecs: List<EnthaerterTypSpec> = listOf(
        EnthaerterTypSpec("5600 FK | Art.Nr. 100406", "5600 FK (Art.Nr.: 100406)", "2,5 Std. (FK/FKL/CGK)"),
        EnthaerterTypSpec("5600 FXM | Art.Nr. 100404", "5600 FXM (Art.Nr.: 100404)", "2,5 Std. (FXM)"),
        EnthaerterTypSpec("9100 FX/GX | Art.Nr. 100408", "9100 FX/GX (Art.Nr.: 100408)", "3,0 Std. (FX/GX)"),
        EnthaerterTypSpec("9100 FXL/GXL | Art.Nr. 100410", "9100 FXL/GXL (Art.Nr.: 100410)", "3,0 Std. (FXL/GXL)"),
        EnthaerterTypSpec("9100 KL&SGX | Art.Nr. 100412", "9100 KL&SGX (Art.Nr.: 100412)", "3,0 Std. (KL / SGX)"),
        EnthaerterTypSpec("FK/CGK/FKL | Art.Nr. 103961", "FK/CGK/FKL (Art.Nr.: 103961)", "2,5 Std. (FK/FKL/CGK)"),
        EnthaerterTypSpec("FXM | Art.Nr. 103965", "FXM (Art.Nr.: 103965)", "2,5 Std. (FXM)"),
        EnthaerterTypSpec("FX/GX/CGX | Art.Nr. 103962", "FX/GX/CGX (Art.Nr.: 103962)", "3,0 Std. (FX/GX/CGX)"),
        EnthaerterTypSpec("FXL/GXL | Art.Nr. 103963", "FXL/GXL (Art.Nr.: 103963)", "3,0 Std. (FXL/GXL)"),
        EnthaerterTypSpec("KL15 | Art.Nr. 103964", "KL15 (Art.Nr.: 103964)", "3,0 Std. (ECO / KL)"),
        EnthaerterTypSpec("KL30 | Art.Nr. 105880", "KL30 (Art.Nr.: 105880)", "3,0 Std. (ECO / KL)"),
        EnthaerterTypSpec("KL50 | Art.Nr. 106009", "KL50 (Art.Nr.: 106009)", "3,0 Std. (ECO / KL)"),
        EnthaerterTypSpec("WAT-100 | Art.Nr. 110566", "WAT-100 (Art.Nr.: 110566)", "3,0 Std. (WAT / SGX)"),
        EnthaerterTypSpec("WAT-200 | Art.Nr. 110757", "WAT-200 (Art.Nr.: 110757)", "3,0 Std. (WAT / SGX)")
    )

    val etMailRecipients: List<EmailRecipient> = listOf(
        EmailRecipient("Sandra Bantle", "sandrabantle@innowatech.de"),
        EmailRecipient("Lisa Widmaier", "lisawidmaier@innowatech.de"),
        EmailRecipient("Jutta Wezel", "juttawezel@innowatech.de"),
        EmailRecipient("Wolfgang Henes", "wolfganghenes@innowatech.de")
    )

    val kundenNotizTypes = listOf(
        "Kundennotiz",
        "Eigennotiz",
        "Wartungshinweis",
        "To-Do / Rückruf",
        "Materialbestellung"
    )

    fun kofferImageRes(boxId: String): Int = when (boxId) {
        "dosierung" -> R.drawable.koffer_dosierung
        "elektrik" -> R.drawable.koffer_elektrik
        "elektrolyse" -> R.drawable.koffer_elektrolyse
        "enthaerter" -> R.drawable.koffer_enthaerter
        "netzteile" -> R.drawable.koffer_netzteile
        "ventile" -> R.drawable.koffer_ventile
        else -> R.drawable.koffer_dosierung
    }

    fun normalizePinNr(boxId: String, nr: String): String {
        val s = nr.trim()
        val pins = kofferPins[boxId].orEmpty()
        if (pins.any { it.nr == s }) return s
        val stripped = s.replace(Regex("[a-zA-Z]+$"), "")
        return if (pins.any { it.nr == stripped }) stripped else s
    }

    fun hashPin(pin: String): String {
        val trimmed = pin.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.length == 64 && trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return trimmed.lowercase(Locale.ROOT)
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(trimmed.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun loadDatabasesFromAssets(context: Context): InnowatechDatabases {
        val jsonStr = context.assets.open("databases.json").bufferedReader().use { it.readText() }
        return parseDatabasesJson(jsonStr)
    }

    fun loadUsersFromAssets(context: Context): Map<String, UserAccount> {
        val jsonStr = context.assets.open("users.json").bufferedReader().use { it.readText() }
        return parseUsersJson(jsonStr)
    }

    fun loadChangelogFromAssets(context: Context): String {
        return try {
            context.assets.open("CHANGELOG.txt").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            CHANGELOG_FALLBACK
        }
    }

    fun parseDatabasesJson(rawJson: String): InnowatechDatabases {
        val root = JSONObject(rawJson)
        val sondenList = mutableListOf<SondeInfo>()
        root.optJSONObject("sonden")?.let { obj ->
            val keys = obj.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
            for (k in keys) {
                val item = obj.optJSONObject(k) ?: continue
                sondenList += SondeInfo(
                    id = k,
                    label = item.optString("label"),
                    messRv1 = item.optString("messRv1"),
                    steilRv1 = item.optString("steilRv1")
                )
            }
        }

        val enthaerterList = mutableListOf<EnthaerterInfo>()
        root.optJSONObject("enthaerter")?.let { obj ->
            for (k in obj.keys()) {
                val item = obj.optJSONObject(k) ?: continue
                enthaerterList += EnthaerterInfo(
                    id = k,
                    label = item.optString("label"),
                    harz = item.optString("harz"),
                    kapazitaet = item.optString("kapazitaet"),
                    sole = item.optString("sole"),
                    salz = item.optString("salz"),
                    rueckfuelle = item.optString("rueckfuelle"),
                    rueckspuelen = item.optString("rueckspuelen"),
                    besalzen = item.optString("besalzen"),
                    schnellspuelen = item.optString("schnellspuelen"),
                    soleNachf = item.optString("sole_nachf")
                )
            }
        }

        val anlagenList = mutableListOf<AnlageInfo>()
        root.optJSONObject("anlagen")?.let { obj ->
            for (k in obj.keys()) {
                val item = obj.optJSONObject(k) ?: continue
                anlagenList += AnlageInfo(
                    id = k,
                    label = item.optString("label"),
                    durchfluss = item.optString("durchfluss"),
                    strom = item.optString("strom"),
                    spannung = item.optString("spannung"),
                    zeit = item.optString("zeit"),
                    prod = item.optString("prod"),
                    pause = item.optString("pause")
                )
            }
        }

        val kofferList = mutableListOf<KofferBox>()
        root.optJSONArray("koffer")?.let { arr ->
            for (i in 0 until arr.length()) {
                val b = arr.optJSONObject(i) ?: continue
                val id = b.optString("id")
                val itemsArr = b.optJSONArray("items") ?: JSONArray()
                val parsedItems = mutableListOf<KofferItem>()
                for (j in 0 until itemsArr.length()) {
                    val row = itemsArr.optJSONArray(j) ?: continue
                    parsedItems += KofferItem(
                        nr = row.optString(0),
                        beschreibung = row.optString(1),
                        hersteller = row.optString(2),
                        hstNr = row.optString(3),
                        inNr = row.optString(4),
                        soll = row.optString(5)
                    )
                }
                kofferList += KofferBox(
                    id = id,
                    name = b.optString("name"),
                    shortName = b.optString("short"),
                    items = parsedItems,
                    imageRes = kofferImageRes(id)
                )
            }
        }

        return InnowatechDatabases(
            sonden = sondenList,
            enthaerter = enthaerterList,
            anlagen = anlagenList,
            koffer = kofferList
        )
    }

    fun serializeDatabasesJson(db: InnowatechDatabases): String {
        val root = JSONObject()
        root.put("version", 1)

        val sondenObj = JSONObject()
        db.sonden.forEach { s ->
            sondenObj.put(
                s.id,
                JSONObject()
                    .put("label", s.label)
                    .put("messRv1", s.messRv1)
                    .put("steilRv1", s.steilRv1)
            )
        }
        root.put("sonden", sondenObj)

        val enthObj = JSONObject()
        db.enthaerter.forEach { e ->
            enthObj.put(
                e.id,
                JSONObject()
                    .put("label", e.label)
                    .put("harz", e.harz)
                    .put("kapazitaet", e.kapazitaet)
                    .put("sole", e.sole)
                    .put("salz", e.salz)
                    .put("rueckfuelle", e.rueckfuelle)
                    .put("rueckspuelen", e.rueckspuelen)
                    .put("besalzen", e.besalzen)
                    .put("schnellspuelen", e.schnellspuelen)
                    .put("sole_nachf", e.soleNachf)
            )
        }
        root.put("enthaerter", enthObj)

        val anlagenObj = JSONObject()
        db.anlagen.forEach { a ->
            anlagenObj.put(
                a.id,
                JSONObject()
                    .put("label", a.label)
                    .put("durchfluss", a.durchfluss)
                    .put("strom", a.strom)
                    .put("spannung", a.spannung)
                    .put("zeit", a.zeit)
                    .put("prod", a.prod)
                    .put("pause", a.pause)
            )
        }
        root.put("anlagen", anlagenObj)

        val kofferArr = JSONArray()
        db.koffer.forEach { box ->
            val itemsArr = JSONArray()
            box.items.forEach { it ->
                itemsArr.put(
                    JSONArray()
                        .put(it.nr)
                        .put(it.beschreibung)
                        .put(it.hersteller)
                        .put(it.hstNr)
                        .put(it.inNr)
                        .put(it.soll)
                )
            }
            kofferArr.put(
                JSONObject()
                    .put("id", box.id)
                    .put("name", box.name)
                    .put("short", box.shortName)
                    .put("items", itemsArr)
            )
        }
        root.put("koffer", kofferArr)
        return root.toString(2)
    }

    fun parseUsersJson(rawJson: String): Map<String, UserAccount> {
        val root = JSONObject(rawJson)
        val result = linkedMapOf<String, UserAccount>()
        for (key in root.keys()) {
            val u = root.optJSONObject(key) ?: continue
            val modArr = u.optJSONArray("modules") ?: JSONArray()
            val mods = buildList {
                for (i in 0 until modArr.length()) {
                    add(modArr.optString(i))
                }
            }
            result[key] = UserAccount(
                kuerzel = key,
                name = u.optString("name", key),
                email = u.optString("email", ""),
                pinHash = u.optString("pinHash", ""),
                status = u.optString("status", "active"),
                isAdmin = u.optBoolean("isAdmin", false),
                autoUpdate = u.optBoolean("autoUpdate", false),
                modules = mods
            )
        }
        return result
    }

    fun serializeUsersJson(users: Map<String, UserAccount>): String {
        val root = JSONObject()
        users.forEach { (k, u) ->
            root.put(
                k,
                JSONObject()
                    .put("name", u.name)
                    .put("email", u.email)
                    .put("pinHash", u.pinHash)
                    .put("status", u.status)
                    .put("isAdmin", u.isAdmin)
                    .put("autoUpdate", u.autoUpdate)
                    .put("modules", JSONArray(u.modules))
            )
        }
        return root.toString(2)
    }

    fun getKnDirectory(users: Map<String, UserAccount>, activeUserKuerzel: String?): List<EmailRecipient> {
        val map = linkedMapOf<String, EmailRecipient>()
        val myEmail = activeUserKuerzel?.let { users[it]?.email?.trim()?.lowercase(Locale.ROOT) }.orEmpty()

        users.forEach { (k, u) ->
            val em = u.email.trim().lowercase(Locale.ROOT)
            if (em.isEmpty()) return@forEach
            val isMe = em == myEmail && myEmail.isNotEmpty()
            val dispName = if (u.name.isNotBlank()) {
                if (isMe && !activeUserKuerzel.isNullOrBlank()) "${u.name} ($activeUserKuerzel)" else u.name
            } else k
            val existing = map[em]
            if (existing == null || isMe) {
                map[em] = EmailRecipient(
                    name = dispName,
                    email = em,
                    kuerzel = if (isMe) (activeUserKuerzel ?: k) else k,
                    isMe = isMe
                )
            }
        }
        listOf(
            EmailRecipient("Sandra Bantle", "sandrabantle@innowatech.de"),
            EmailRecipient("Lisa Widmaier", "lisawidmaier@innowatech.de"),
            EmailRecipient("Jutta Wezel", "juttawezel@innowatech.de"),
            EmailRecipient("Service-Hotline", HOTLINE_EMAIL)
        ).forEach { extra ->
            if (!map.containsKey(extra.email)) {
                map[extra.email] = extra
            }
        }
        return map.values.sortedWith(
            compareByDescending<EmailRecipient> { it.isMe }
                .thenBy { it.name.lowercase(Locale.GERMAN) }
        )
    }

    fun getEtTypTime(typValue: String): String? =
        enthaerterTypSpecs.find { it.value == typValue }?.timeText

    fun buildEnthaertertauschText(state: EnthaerterChecklistState): String {
        fun g(s: String): String = s.trim().ifEmpty { "-" }
        fun customOrBase(base: String, custom: String): String {
            val b = base.trim()
            if (b.isEmpty()) return "-"
            if (b == "Sonstiges") {
                val c = custom.trim()
                return if (c.isNotEmpty()) "Sonstiges: $c" else "Sonstiges"
            }
            return b
        }
        val zeit = getEtTypTime(state.typ) ?: "-"
        val turbStr = when (val t = state.turbine.trim()) {
            "" -> "-"
            "Ohne" -> "Ohne / Keine verbaut"
            else -> "Nr. $t"
        }
        return listOf(
            "AUFNAHME ENTHÄRTERTAUSCH",
            "----------------------------------------",
            "Kürzel Mitarbeiter: ${g(state.kuerzel)}",
            "Kundenname: ${g(state.kunde)}",
            "PLZ & Ort: ${g(state.ort)}",
            "Fabrikationsnummer: ${g(state.fabnr)}",
            "Anlagentyp: ${g(state.anlage)}",
            "Enthärtertyp: ${g(state.typ)}",
            "Zeitaufwand (mit Reg.): $zeit",
            "Kabinett auch erneuern: ${g(state.kabinettNeu)}",
            "Kabinett-Art: ${g(state.kabinettArt)}",
            "Schlauch Enthärter → Hauptventil (AD): ${customOrBase(state.schlauchHv, state.schlauchHvCustom)}",
            "Schlauch zum Hauptventil erneuern: ${g(state.schlauchHvNeu)}",
            "Schlauch Enthärter → Abfluss (ID): ${customOrBase(state.schlauchAbfluss, state.schlauchAbflussCustom)}",
            "Panzerschlauch Größe: ${customOrBase(state.panzerGroesse, state.panzerGroesseCustom)}",
            "Panzerschlauch Länge: ${customOrBase(state.panzerLaenge, state.panzerLaengeCustom)}",
            "Panzerschlauch Anschlussart: ${customOrBase(state.panzerAnschluss, state.panzerAnschlussCustom)}",
            "Extra Schwimmerlanze (Solesättigung): ${g(state.schwimmerlanze)}",
            "Durchflussmesser / Turbine: $turbStr",
            "Fotos (alle Seiten/Bauteile/Rotgussfittings): ${g(state.fotos)}",
            "Bemerkungen: ${g(state.bemerkung)}"
        ).joinToString("\n")
    }

    fun formatGermanDateTime(iso: String): String {
        if (iso.isBlank()) return ""
        return try {
            val epochMs = iso.toLongOrNull()
            val date = if (epochMs != null) {
                Date(epochMs)
            } else {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                parser.parse(iso.take(19))
            }
            if (date == null) "" else SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMAN).format(date)
        } catch (_: Exception) {
            ""
        }
    }

    fun nowTimestampChip(): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.GERMAN)
        return "[${formatter.format(Date())}]"
    }

    fun formatIsoToGermanDate(iso: String): String {
        if (iso.isBlank()) return ""
        val parts = iso.split("-")
        return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else iso
    }

    fun formatMillisToIsoDate(utcMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(utcMillis))
    }

    fun nowIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date())
    }

    fun serializeEtRecords(records: List<SavedEnthaerterRecord>): String {
        val arr = JSONArray()
        records.forEach { rec ->
            val st = rec.state
            val stObj = JSONObject()
                .put("recordId", st.recordId ?: rec.id)
                .put("kuerzel", st.kuerzel)
                .put("fabnr", st.fabnr)
                .put("kunde", st.kunde)
                .put("ort", st.ort)
                .put("anlage", st.anlage)
                .put("typ", st.typ)
                .put("kabinettNeu", st.kabinettNeu)
                .put("kabinettArt", st.kabinettArt)
                .put("schlauchHv", st.schlauchHv)
                .put("schlauchHvCustom", st.schlauchHvCustom)
                .put("schlauchHvNeu", st.schlauchHvNeu)
                .put("schlauchAbfluss", st.schlauchAbfluss)
                .put("schlauchAbflussCustom", st.schlauchAbflussCustom)
                .put("panzerGroesse", st.panzerGroesse)
                .put("panzerGroesseCustom", st.panzerGroesseCustom)
                .put("panzerLaenge", st.panzerLaenge)
                .put("panzerLaengeCustom", st.panzerLaengeCustom)
                .put("panzerAnschluss", st.panzerAnschluss)
                .put("panzerAnschlussCustom", st.panzerAnschlussCustom)
                .put("schwimmerlanze", st.schwimmerlanze)
                .put("turbine", st.turbine)
                .put("fotos", st.fotos)
                .put("bemerkung", st.bemerkung)
            arr.put(
                JSONObject()
                    .put("id", rec.id)
                    .put("title", rec.title)
                    .put("updatedAtIso", rec.updatedAtIso)
                    .put("state", stObj)
            )
        }
        return arr.toString()
    }

    fun parseEtRecords(raw: String): List<SavedEnthaerterRecord> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val stObj = obj.optJSONObject("state") ?: continue
                    val id = obj.optString("id")
                    add(
                        SavedEnthaerterRecord(
                            id = id,
                            title = obj.optString("title"),
                            updatedAtIso = obj.optString("updatedAtIso"),
                            state = EnthaerterChecklistState(
                                recordId = id,
                                kuerzel = stObj.optString("kuerzel"),
                                fabnr = stObj.optString("fabnr"),
                                kunde = stObj.optString("kunde"),
                                ort = stObj.optString("ort"),
                                anlage = stObj.optString("anlage"),
                                typ = stObj.optString("typ"),
                                kabinettNeu = stObj.optString("kabinettNeu"),
                                kabinettArt = stObj.optString("kabinettArt"),
                                schlauchHv = stObj.optString("schlauchHv"),
                                schlauchHvCustom = stObj.optString("schlauchHvCustom"),
                                schlauchHvNeu = stObj.optString("schlauchHvNeu"),
                                schlauchAbfluss = stObj.optString("schlauchAbfluss"),
                                schlauchAbflussCustom = stObj.optString("schlauchAbflussCustom"),
                                panzerGroesse = stObj.optString("panzerGroesse"),
                                panzerGroesseCustom = stObj.optString("panzerGroesseCustom"),
                                panzerLaenge = stObj.optString("panzerLaenge"),
                                panzerLaengeCustom = stObj.optString("panzerLaengeCustom"),
                                panzerAnschluss = stObj.optString("panzerAnschluss"),
                                panzerAnschlussCustom = stObj.optString("panzerAnschlussCustom"),
                                schwimmerlanze = stObj.optString("schwimmerlanze"),
                                turbine = stObj.optString("turbine"),
                                fotos = stObj.optString("fotos"),
                                bemerkung = stObj.optString("bemerkung")
                            )
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeKnArchive(entries: List<KundenNotizEntry>): String {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("typ", e.typ)
                    .put("sn", e.sn)
                    .put("kunde", e.kunde)
                    .put("betreff", e.betreff)
                    .put("text", e.text)
                    .put("author", e.author)
                    .put("createdAtIso", e.createdAtIso)
                    .put("updatedAtIso", e.updatedAtIso)
            )
        }
        return arr.toString()
    }

    fun parseKnArchive(raw: String): List<KundenNotizEntry> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        KundenNotizEntry(
                            id = o.optString("id"),
                            typ = o.optString("typ", "Kundennotiz"),
                            sn = o.optString("sn"),
                            kunde = o.optString("kunde"),
                            betreff = o.optString("betreff"),
                            text = o.optString("text"),
                            author = o.optString("author", "-"),
                            createdAtIso = o.optString("createdAtIso"),
                            updatedAtIso = o.optString("updatedAtIso")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    const val CHANGELOG_FALLBACK = """
INNOWATΞCH Service Tool – Kumulatives Changelog (#MAT)

Neu in v5.6 (Top-Reset, Enthärter-Zeitaufwand & Verteiler)
• Kompakter Reset- / „Neu“-Button direkt oben in der Zurück-Leiste neben „Zurück zur Übersicht“
• Checkliste Enthärtertausch: Exakter kalkulatorischer Zeitaufwand direkt im Datenblatt
• E-Mail-Verteiler (Checkliste & Kunden-/Eigennotizen) für Outlook & Handy

Neu in v5.5 (Dashboard als Hauptstartseite & 3er-Kompaktraster)
• Feste Hauptstartseite Dashboard mit VEGA Tools Struktur & Übersicht
• Dashboard-Kacheldichte wählbar: Kompakt (3er-Raster) oder Groß (2er-Raster)

Neu in v5.4 (Checkliste-Verteiler, Kundennotizen, Dosierauswertung & DB)
• Checkliste Enthärtertausch mit drucksicherer Auswahl, Sonstiges-Eingabe, Ohne-Turbine & Verteiler
• Modul „Kunden- & Eigennotizen“ mit lokalem Archiv, PDF & Kollegen-Mail
• Modul „Dosierauswertung“ inkl. Tageswerten & Übernahme in Service-Notizen
• VEGA Radarsensor mit Tankdurchmesser (Ø) und hervorgehobenem Tank in Schritt 4

Neu in v5.3 (Katalog & Checklisten, Nachorder-Menge)
• Servicekoffer (6 L-Boxen, 162 Artikel) mit interaktiven Fach-Pins & Nachorder-Menge
• 4 Kategorien: Anwendung, Parameter, Hilfsmittel, Katalog & Checklisten
"""
}
