package se.matb.turf.route.dao.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import kotlin.test.Test

class RouteInfoTest {

    @Test
    fun `times should read string`() {
        val input = "5:1,6:1,7:3,8:3,9:2"
        val times = RouteInfo.Times(input)
        val expected = listOf(Pair(5, 1), Pair(6, 1), Pair(7, 3), Pair(8, 3), Pair(9, 2))
        assertThat(times.times).isEqualTo(expected)
    }

    @Test
    fun `times should read old format`() {
        val input = "5,6,7,7,7,8,8,8,9,9"
        val times = RouteInfo.Times(input)
        val expected = listOf(Pair(5, 1), Pair(6, 1), Pair(7, 3), Pair(8, 3), Pair(9, 2))
        assertThat(times.times).isEqualTo(expected)
    }

    @Test
    fun `times should read mixed format`() {
        val input = "5,6,7:3,8:3,9:2,10"
        val times = RouteInfo.Times(input)
        val expected = listOf(Pair(5, 1), Pair(6, 1), Pair(7, 3), Pair(8, 3), Pair(9, 2), Pair(10, 1))
        assertThat(times.times).isEqualTo(expected)
    }

    @Test
    fun `times should get from correct index`() {
        val input = "5,6,7:2,10"
        val times = RouteInfo.Times(input)
        assertThat(times.get(0)).isEqualTo(5)
        assertThat(times.get(1)).isEqualTo(6)
        assertThat(times.get(2)).isEqualTo(7)
        assertThat(times.get(3)).isEqualTo(7)
        assertThat(times.get(4)).isEqualTo(10)
        assertThatThrownBy { times.get(5) }.isInstanceOf(IndexOutOfBoundsException::class.java)
    }

    @Test
    fun `times should write to mixed format`() {
        val input = "5,6,7:3,8:3,9:2,10"
        val times = RouteInfo.Times(input)
        assertThat(times.toString()).isEqualTo(input)
    }

    @Test
    fun `times should have correct size`() {
        val input = "5,6,7:3,8:3,9:2,10"
        val times = RouteInfo.Times(input)
        assertThat(times.size).isEqualTo(11)
    }

    @Test
    fun `times should have correct fastest time`() {
        val input = "5,6,7:3,8:3,9:2,10"
        val times = RouteInfo.Times(input)
        assertThat(times.fastest()).isEqualTo(5)
    }

    @Test
    fun `times should have correct average`() {
        val input = "5,6,7:3,8:3,9:2,10"
        val times = RouteInfo.Times(input)
        assertThat(times.avg()).isEqualTo(7)
    }

    @Test
    fun `times should have correct median odd size`() {
        val input = "5,6,7:3,8:3,9:2,10"
        val times = RouteInfo.Times(input)
        assertThat(times.med()).isEqualTo(8)
    }

    @Test
    fun `times should have correct median even size`() {
        val input = "5,6,7:3,8:3,9:2"
        val times = RouteInfo.Times(input)
        assertThat(times.med()).isEqualTo(7)
    }

    @Test
    fun `test sorted insert large after small`() {
        val ri = RouteInfo(1, 2, RouteInfo.Times("79"))
        ri.times.add(107)
        val sorted = ri.times.times.sortedBy { it.first }
        assertThat(ri.times.times).containsExactlyElementsOf(sorted)
    }

    @Test
    fun `test sorted insert small after large`() {
        val ri = RouteInfo(1, 2, RouteInfo.Times("107"))
        ri.times.add(79)
        val sorted = ri.times.times.sortedBy { it.first }
        assertThat(ri.times.times).containsExactlyElementsOf(sorted)
    }

    @Test
    fun `test sorted insert 4 different from middle`() {
        val ri = RouteInfo(1, 2, RouteInfo.Times("79"))
        ri.times.add(107)
        ri.times.add(88)
        ri.times.add(99)
        val sorted = ri.times.times.sortedBy { it.first }
        assertThat(ri.times.times).containsExactlyElementsOf(sorted)
    }

    @Test
    fun `test sorted insert 4 different from edges`() {
        val ri = RouteInfo(1, 2, RouteInfo.Times("99"))
        ri.times.add(88)
        ri.times.add(79)
        ri.times.add(107)
        val sorted = ri.times.times.sortedBy { it.first }
        assertThat(ri.times.times).containsExactlyElementsOf(sorted)
    }

    @Test
    fun `test sorted insert duplicate in middle`() {
        val ri = RouteInfo(1, 2, RouteInfo.Times("99"))
        ri.times.add(88)
        ri.times.add(79)
        ri.times.add(107)
        ri.times.add(88)
        val sorted = ri.times.times.sortedBy { it.first }
        assertThat(ri.times.times).containsExactlyElementsOf(sorted)
    }

    @Test
    fun `test sorted insert duplicate at start`() {
        val ri = RouteInfo(1, 2, RouteInfo.Times("99"))
        ri.times.add(88)
        ri.times.add(79)
        ri.times.add(107)
        ri.times.add(79)
        val sorted = ri.times.times.sortedBy { it.first }
        assertThat(ri.times.times).containsExactlyElementsOf(sorted)
    }

    @Test
    fun `test sorted insert duplicate at end`() {
        val ri = RouteInfo(1, 2, RouteInfo.Times("99"))
        ri.times.add(88)
        ri.times.add(107)
        ri.times.add(79)
        ri.times.add(107)
        val sorted = ri.times.times.sortedBy { it.first }
        assertThat(ri.times.times).containsExactlyElementsOf(sorted)
    }
}
