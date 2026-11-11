package com.kostku.app

import com.kostku.app.util.Fmt
import com.kostku.app.util.PhoneUtils
import com.kostku.app.util.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilTest {

    @Test fun terbilang_delapanRatusRibu() {
        assertEquals("Delapan Ratus Ribu Rupiah", Fmt.terbilang(800_000))
    }

    @Test fun terbilang_variasi() {
        assertEquals("Seribu Rupiah", Fmt.terbilang(1_000))
        assertEquals("Satu Juta Lima Ratus Ribu Rupiah", Fmt.terbilang(1_500_000))
        assertEquals("Sebelas Ribu Rupiah", Fmt.terbilang(11_000))
        assertEquals("Seratus Dua Puluh Lima Rupiah", Fmt.terbilang(125))
    }

    @Test fun phone_normalisasi() {
        assertEquals("081234567890", PhoneUtils.normalize("+62 812-3456-7890"))
        assertEquals("081234567890", PhoneUtils.normalize("6281234567890"))
        assertEquals("6281234567890", PhoneUtils.toInternational("081234567890"))
    }

    @Test fun validator_nikDanHp() {
        assertTrue(Validators.isValidNik("7371010101000102"))
        assertFalse(Validators.isValidNik("12345"))
        assertFalse(Validators.isValidNik("73710101010001AB"))
        assertTrue(Validators.isValidPhone("0812-3456-7890"))
        assertFalse(Validators.isValidPhone("12345"))
    }
}
