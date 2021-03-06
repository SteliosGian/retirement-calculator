package retcalc

import org.scalactic.{Equality, TolerantNumerics, TypeCheckedTripleEquals}
import org.scalatest.{Matchers, WordSpec, EitherValues}



class RetCalcSpec extends WordSpec with Matchers with TypeCheckedTripleEquals with EitherValues{

  implicit val doubleEquality: Equality[Double] =
    TolerantNumerics.tolerantDoubleEquality(0.0001)

  val params = RetCalcParams(
    nbOfMonthsInRetirement = 40 * 12,
    netIncome = 3000,
    currentExpenses = 2000,
    initialCapital = 10000
  )

  "RetCalc.futureCapital" should {
    "calculate the amount of savings I will have in n months" in {
      // Excel = -FV(0.04/12,25*12,1000,10000,0)
      val actual = RetCalc.futureCapital(FixedReturns(0.04),
        nbOfMonths = 25 * 12, netIncome=3000,
        currentExpenses = 2000, initialCapital = 10000).right.value
      val expected = 541267.1990
      actual should === (expected)
    }
  }

  it should {
    "calculate how much savings will be left after having taken a pension for n months" in {
      val actual = RetCalc.futureCapital(FixedReturns(0.04),
        nbOfMonths = 40 * 12, netIncome = 0, currentExpenses = 2000,
        initialCapital = 541267.198962).right.value
      val expected = 309867.5316
      actual should === (expected)
    }
  }

  "RetCalc.simulatePlan" should {
    "calculate the capital at retirement and the capital after death" in {
      val (capitalAtRetirement, capitalAfterDeath) =
        RetCalc.simulatePlan(
          returns = FixedReturns(0.04), params, nbOfMonthsSavings = 25 * 12
        ).right.value
      capitalAtRetirement should === (541267.1990)
      capitalAfterDeath should === (309867.5316)
    }

    "use different returns for capitalisation and drawdown" in {
      val nbOfMonthsSavings = 25 * 12
      val returns = VariableReturns(
        Vector.tabulate(nbOfMonthsSavings + params.nbOfMonthsInRetirement) (i =>
        if (i < nbOfMonthsSavings)
        VariableReturn(i.toString, 0.04 / 12)
        else
        VariableReturn(i.toString, 0.03 / 12)))

        val (capitalAtRetirement, capitalAfterDeath) =
          RetCalc.simulatePlan(returns, params, nbOfMonthsSavings).right.value
      capitalAtRetirement should === (541267.1990)
      capitalAfterDeath should === (-57737.7227)
    }
  }

  "RetCalc.nbOfMonthsSaving" should {
    "calculate how long I need to save before I can retire" in {
      val actual = RetCalc.nbOfMonthsSaving(params, FixedReturns(0.04)).right.value
      val expected = 23*12+1
      actual should === (expected)
    }
  }

  it should {
    "not crash if the resulting nbOfMonths is very high" in {
      val actual = RetCalc.nbOfMonthsSaving(params = RetCalcParams(nbOfMonthsInRetirement = 40*12,
        netIncome = 3000, currentExpenses = 2999, initialCapital = 0),
        returns = FixedReturns(0.01)).right.value
      val expected = 8280
      actual should === (expected)
    }
    "not loop forever if I enter bad parameters" in {
      val actual = RetCalc.nbOfMonthsSaving(params.copy(netIncome = 1000), FixedReturns(0.04)).left.value
      actual should === (RetCalcError.MoreExpensesThanIncome(1000, 2000))
    }
  }
}
