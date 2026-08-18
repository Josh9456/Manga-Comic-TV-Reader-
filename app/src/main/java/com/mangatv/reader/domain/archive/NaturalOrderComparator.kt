package com.mangatv.reader.domain.archive

import java.util.Comparator

/**
 * Natural sort comparator that handles embedded numbers in strings correctly:
 * e.g., "page1.jpg", "page2.jpg", "page10.jpg" instead of alphabetical "page10.jpg" before "page2.jpg"
 */
class NaturalOrderComparator : Comparator<String> {

    override fun compare(a: String?, b: String?): Int {
        if (a == null && b == null) return 0
        if (a == null) return -1
        if (b == null) return 1

        var ia = 0
        var ib = 0
        val nza = a.length
        val nzb = b.length

        while (ia < nza && ib < nzb) {
            val ca = a[ia]
            val cb = b[ib]

            if (ca.isDigit() && cb.isDigit()) {
                var startA = ia
                var startB = ib
                while (startA < nza && a[startA] == '0') startA++
                while (startB < nzb && b[startB] == '0') startB++

                var endA = startA
                var endB = startB
                while (endA < nza && a[endA].isDigit()) endA++
                while (endB < nzb && b[endB].isDigit()) endB++

                val lengthA = endA - startA
                val lengthB = endB - startB

                if (lengthA != lengthB) {
                    return lengthA - lengthB
                }

                while (startA < endA) {
                    if (a[startA] != b[startB]) {
                        return a[startA] - b[startB]
                    }
                    startA++
                    startB++
                }

                ia = endA
                ib = endB
            } else {
                val comp = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (comp != 0) return comp
                ia++
                ib++
            }
        }
        return (nza - ia) - (nzb - ib)
    }
}
