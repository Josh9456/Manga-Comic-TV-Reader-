package com.mangatv.reader

import com.mangatv.reader.domain.archive.NaturalOrderComparator
import org.junit.Assert.assertEquals
import org.junit.Test

class NaturalOrderComparatorTest {

    @Test
    fun testNaturalOrderSorting() {
        val input = listOf(
            "page10.png",
            "page1.png",
            "page2.png",
            "page100.png",
            "page20.png",
            "page3.png"
        )

        val comparator = NaturalOrderComparator()
        val sorted = input.sortedWith(comparator)

        val expected = listOf(
            "page1.png",
            "page2.png",
            "page3.png",
            "page10.png",
            "page20.png",
            "page100.png"
        )

        assertEquals(expected, sorted)
    }

    @Test
    fun testZeroPaddedSorting() {
        val input = listOf(
            "001.jpg",
            "010.jpg",
            "002.jpg",
            "020.jpg",
            "003.jpg"
        )

        val comparator = NaturalOrderComparator()
        val sorted = input.sortedWith(comparator)

        val expected = listOf(
            "001.jpg",
            "002.jpg",
            "003.jpg",
            "010.jpg",
            "020.jpg"
        )

        assertEquals(expected, sorted)
    }
}
