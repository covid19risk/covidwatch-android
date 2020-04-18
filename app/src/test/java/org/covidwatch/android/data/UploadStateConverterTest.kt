package org.covidwatch.android.data

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

import org.assertj.core.api.Assertions.*
import java.lang.IllegalArgumentException

internal class UploadStateConverterTest {

    private val uploadStateConv: UploadStateConverter = UploadStateConverter()

    @Nested
    inner class toUploadState() {

        @Test
        fun `check conversion to NOTUPLOADED`() {
            assertThat(uploadStateConv.toUploadState(ContactEvent.UploadState.NOTUPLOADED.code))
                .isEqualTo(ContactEvent.UploadState.NOTUPLOADED)
        }
        @Test
        fun `check conversion to UPLOADING`() {
            assertThat(uploadStateConv.toUploadState(ContactEvent.UploadState.UPLOADING.code))
                .isEqualTo(ContactEvent.UploadState.UPLOADING)
        }
        @Test
        fun `check conversion to UPLOADED`() {
            assertThat(uploadStateConv.toUploadState(ContactEvent.UploadState.UPLOADED.code))
                .isEqualTo(ContactEvent.UploadState.UPLOADED)
        }
        @Test
        fun `check IllegalArgumentException`() {
            assertThatThrownBy {uploadStateConv.toUploadState(-1)}
                .isExactlyInstanceOf(IllegalArgumentException::class.java)
        }

    }

    @Nested
    inner class toInteger() {

        @Test
        fun `check conversion from NOTUPLOADED`() {
            assertThat(uploadStateConv.toInteger(ContactEvent.UploadState.NOTUPLOADED))
                .isEqualTo(ContactEvent.UploadState.NOTUPLOADED.code)
        }
        @Test
        fun `check conversion from UPLOADING`() {
            assertThat(uploadStateConv.toInteger(ContactEvent.UploadState.UPLOADING))
                .isEqualTo(ContactEvent.UploadState.UPLOADING.code)
        }
        @Test
        fun `check conversion from UPLOADED`() {
            assertThat(uploadStateConv.toInteger(ContactEvent.UploadState.UPLOADED))
                .isEqualTo(ContactEvent.UploadState.UPLOADED.code)
        }

    }
}