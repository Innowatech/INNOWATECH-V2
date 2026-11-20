package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.InnowatechCalculations
import com.example.data.InnowatechRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Innotech Tool", appName)
    }

    @Test
    fun `users and databases assets load real INNOWATECH data`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val users = InnowatechRepository.loadUsersFromAssets(context)
        assertEquals(15, users.size)
        assertNotNull(users["ADMIN"])
        assertTrue(users["ADMIN"]?.isAdmin == true)
        assertNotNull(users["MAT"])
        assertEquals(17, users["MAT"]?.modules?.size)
        assertEquals(
            "9af15b336e6a9619928537df30b2e6a2376569fcf9d7e773eccede65606529a0",
            InnowatechRepository.hashPin("0000")
        )
        assertEquals(
            users["MAT"]?.pinHash?.lowercase(),
            InnowatechRepository.hashPin(users["MAT"]!!.pinHash)
        )

        val dbs = InnowatechRepository.loadDatabasesFromAssets(context)
        assertEquals(11, dbs.sonden.size)
        assertEquals(11, dbs.enthaerter.size)
        assertEquals(47, dbs.anlagen.size)
        assertEquals(6, dbs.koffer.size)
        assertEquals(162, dbs.koffer.sumOf { it.items.size })
        dbs.koffer.forEach { box ->
            assertTrue("Pins missing for ${box.id}", InnowatechRepository.kofferPins[box.id]?.isNotEmpty() == true)
        }
    }

    @Test
    fun `all core INNOWATECH formulas match v5_6 logic`() {
        val std = InnowatechCalculations.calcStandard(ziel = 0.4, volL = 1000.0, anolMgL = 500.0, zehrung = 0.1)
        assertEquals(0.5, std.sollMgL, 0.001)
        assertEquals(1000.0, std.mengeMl, 0.1)

        val ww = InnowatechCalculations.calcWasserwerk(
            m3Tag = 240.0,
            anolKonz = 500.0,
            ziel = 0.3,
            zehrung = 0.1,
            impulsL = 10.0,
            maxFlowM3h = 20.0
        )
        assertEquals(0.4, ww.konzMgL, 0.001)
        assertEquals(192.0, ww.bedarfLTag, 0.01)
        assertEquals(8.0, ww.hubMl, 0.01)
        assertEquals(0.8, ww.lProM3, 0.001)
        assertEquals(16.0, ww.pumpe20MaLh, 0.01)

        val pool = InnowatechCalculations.calcSchwimmbad(
            volumenL = 50000.0,
            chlorsorte = "anolyte",
            chlorIst = 0.2,
            chlorSoll = 0.6,
            chlorKonz = 500.0,
            phIst = 7.6,
            phSoll = 7.2,
            alk = 100.0,
            saeureKonz = 20.0
        )
        assertTrue(pool.chlorText.contains("40.00 l"))
        assertTrue(pool.phText.contains("3.00 l / kg"))

        val pid = InnowatechCalculations.calcPid(
            betriebsart = "kalt_kombiniert",
            testL = 10.0,
            wirkstoff = 500.0,
            chlorDiff = 0.5,
            tu = 30.0,
            tg = 60.0,
            grundwert = null,
            sollwert = null
        )
        assertTrue(pid.valid)
        assertEquals("5.0 %", pid.pText)
        assertEquals("150 s", pid.iText)

        val verd = InnowatechCalculations.calcVerduennung(
            anlagentyp = "500",
            customKonz = null,
            customZiel = null,
            messwert = 250.0
        )
        assertEquals("1:1 (Faktor 2)", verd.stufeText)
        assertEquals("500.0 mg/l (ppm)", verd.finalText)

        val dos = InnowatechCalculations.calcDosierauswertung(
            wzAlt = 100.0,
            wzNeu = 200.0,
            dosAlt = 50.0,
            dosNeu = 150.0,
            konz = 500.0,
            soll = 0.5,
            dateAltIso = "2026-01-01",
            dateNeuIso = "2026-01-11"
        )
        assertEquals("100.00 m³", dos.wasserText)
        assertEquals("100.00 l", dos.anolytText)
        assertEquals("0.10 %", dos.quoteText)
        assertEquals("0.500 mg/l", dos.chlorText)
        assertEquals("100 %", dos.faktorText)
        assertEquals("10 Tage", dos.tageText)

        val schlauch = InnowatechCalculations.calcSchlauch(
            dMm = 10.0,
            lM = 10.0,
            pumpEnabled = true,
            pumpLh = 0.7854
        )
        assertEquals("0.79 l", schlauch.gesamtText)

        val enthaerterList = InnowatechRepository.loadDatabasesFromAssets(
            ApplicationProvider.getApplicationContext()
        ).enthaerter
        val e80 = InnowatechCalculations.calcEnthaerter80(enthaerterList.first(), 10.0)
        assertTrue(e80.kap80Text.contains("°dH·m³"))
        assertTrue(e80.literText.contains("l ("))
    }
}
