package com.example

import com.example.marketedge.data.MarketRepository
import com.example.marketedge.model.EarningsVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun verifyMarketPulseAndScreenerData() {
    val pulse = MarketRepository.getMarketPulse()
    assertTrue(pulse.bullets.isNotEmpty())
    assertTrue(pulse.fiiDiiFlows.isNotEmpty())
    assertTrue(pulse.breadth.totalStocks > 0)

    // Verify official up-to-date Nifty 50 and Nifty 500 levels (Nov 12, 2026 verified session: Nifty 50 25,299.35, Nifty 500 23,779.30)
    assertTrue(pulse.nifty50Level in 24500.0..26500.0)
    assertTrue(pulse.nifty500Level in 23000.0..24500.0)
    assertTrue(pulse.asOfDate.contains("12-Nov-2026"))

    val indices = MarketRepository.getIndices()
    val n50 = indices.firstOrNull { it.id == "nifty50" }
    val n500 = indices.firstOrNull { it.id == "nifty500" }
    assertTrue(n50 != null && n50.currentPrice in 24500.0..26500.0)
    assertTrue(n500 != null && n500.currentPrice in 23000.0..24500.0)

    val screener = MarketRepository.getScreenerStocks()
    assertTrue(screener.size >= 20)
    assertTrue(screener.all { it.symbol.isNotBlank() && it.currentPrice > 0.0 && it.rsScore in 1..99 })
    
    // Check specific Nifty 500 momentum leaders exist
    val symbols = screener.map { it.symbol }
    assertTrue(symbols.contains("DIXON"))
    assertTrue(symbols.contains("TRENT"))
    assertTrue(symbols.contains("ZOMATO"))
    assertTrue(symbols.contains("TITAGARH"))
    assertTrue(symbols.contains("VBL"))

    val topStock = screener.maxByOrNull { it.rsScore }
    assertTrue(topStock != null && topStock.rsScore >= 95)
    assertTrue(topStock!!.threeMonthReturnPercent > topStock.nifty500ThreeMonthReturn)

    val earnings = MarketRepository.getEarningsResults()
    assertTrue(earnings.isNotEmpty())
    assertTrue(earnings.any { it.verdict == EarningsVerdict.STRONG_BEAT })
  }

  @Test
  fun verifyInstitutionalFlows() {
    val flows = MarketRepository.getMarketPulse().fiiDiiFlows
    assertTrue(flows.size >= 5)
    val latest = flows.first()
    assertEquals(latest.fiiNetCr + latest.diiNetCr, latest.totalNetCr, 0.01)
    // Check FII net buying surge in November 2026 series
    assertTrue(flows.any { it.fiiNetCr > 0 })
    assertEquals(12563.33, latest.fiiNetCr, 0.01)
    assertEquals(-8434.53, latest.diiNetCr, 0.01)
  }

  @Test
  fun verifyUpcomingEarningsAndVolatility() {
    val upcoming = MarketRepository.getUpcomingEarnings()
    assertTrue(upcoming.isNotEmpty())
    assertTrue(upcoming.any { it.volatility.level == 3 }) // High volatility
    assertTrue(upcoming.any { it.volatility.level == 2 }) // Medium volatility
    assertTrue(upcoming.any { it.volatility.level == 1 }) // Low volatility
    // Check implied moves are positive
    assertTrue(upcoming.all { it.impliedMovePercent > 0.0 })
    // Check dates are populated
    assertTrue(upcoming.all { it.expectedDate.isNotBlank() && it.companyName.isNotBlank() })
  }

  @Test
  fun verifyQuantStatistics() {
    val stocks = MarketRepository.getScreenerStocks()
    assertTrue(stocks.isNotEmpty())
    stocks.forEach { stock ->
      val qs = stock.quantStats
      assertTrue(qs.standardDeviationAnnualized > 10.0)
      assertTrue(qs.standardDeviationDaily > 0.5)
      assertTrue(qs.correlationNifty500 in -1.0..1.0)
      assertTrue(qs.correlationSector in -1.0..1.0)
      assertTrue(qs.correlationNifty50 in -1.0..1.0)
      assertTrue(qs.betaNifty500 > 0.5)
      assertTrue(qs.oneSigmaPriceLow < stock.currentPrice)
      assertTrue(qs.oneSigmaPriceHigh > stock.currentPrice)
      assertTrue(qs.twoSigmaPriceLow < qs.oneSigmaPriceLow)
      assertTrue(qs.twoSigmaPriceHigh > qs.oneSigmaPriceHigh)
    }
  }

  @Test
  fun verifyTechnicalIndicatorsRsiAndMacd() {
    val stocks = MarketRepository.getScreenerStocks()
    assertTrue(stocks.isNotEmpty())
    stocks.forEach { stock ->
      assertTrue("Candles must be populated for ${stock.symbol}", stock.candles.isNotEmpty())
      stock.candles.forEach { candle ->
        // RSI is bounded within [0, 100]
        assertTrue("RSI must be between 0 and 100", candle.rsi in 0f..100f)
        // MACD histogram equals MACD - Signal
        val expectedHist = candle.macd - candle.macdSignal
        assertEquals(expectedHist, candle.macdHist, 0.05f)
      }
    }
  }
}

