/*
 * Copyright 2012 Glenn Heinze .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.accounted4.money.loan;

import com.accounted4.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Iterator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Glenn Heinze 
 */
public class AmortizationCalculatorTest {
    
    public AmortizationCalculatorTest() {
    }

    /**
     * Test of getPayments method, of class AmortizationCalculator.
     */
    @Test
    public void testGetPaymentsInterestOnly() {
        System.out.println("getPaymentsInterestOnly");
        
        Money amount = new Money("100.00");
        double rate = 12.0;

        AmortizationAttributes terms = new AmortizationAttributes();
        terms.setInterestOnly(true);
        terms.setLoanAmount(amount);
        terms.setTermInMonths(12);
        terms.setInterestRate(rate);
        double interestOnlyMonthlyPayment = AmortizationCalculator.getInterestOnlyMonthlyPayment(amount.getAmount().doubleValue(), rate);
        terms.setRegularPayment(new Money(BigDecimal.valueOf(interestOnlyMonthlyPayment)));
        
        LocalDate today = LocalDate.now();
        LocalDate adjDate = LocalDate.now();
        
        int dayOfMonth = today.getDayOfMonth();
        if (1 != dayOfMonth) {
            if (dayOfMonth > 15) {
              adjDate = LocalDate.of(today.getYear(), today.getMonthValue(), 1);
              adjDate = adjDate.plusMonths(1);
            } else if (dayOfMonth < 15) {
              adjDate = LocalDate.of(today.getYear(), today.getMonthValue(), 15);
            }
        }
        
        terms.setStartDate(today);
        terms.setAdjustmentDate(adjDate);
        
        Iterator<ScheduledPayment> result = AmortizationCalculator.getPayments(terms);
        
        int resultCount = 0;
        Money interestTotal = new Money("0.00");
        while(result.hasNext()) {
            resultCount++;
            ScheduledPayment payment = result.next();
            interestTotal = interestTotal.add(payment.getInterest());
        }
        assertEquals("Interest Only payment count", 12, resultCount);
        assertEquals("Interest Only interest total", new Money("12"), interestTotal);

    }

    
    
    @Test
    public void testGetPaymentsAmortized() {
        System.out.println("testGetPaymentsAmortized");
        
        Money amount = new Money("200000.00", Currency.getInstance("CAD"), RoundingMode.HALF_UP);
        double rate = 8.0;
        int termInMonths = 36;
        
        AmortizationAttributes terms = new AmortizationAttributes();
        terms.setInterestOnly(false);
        terms.setLoanAmount(amount);
        terms.setTermInMonths(termInMonths);
        terms.setAmortizationPeriodMonths(20 * 12);
        terms.setCompoundingPeriodsPerYear(2);
        terms.setInterestRate(rate);
        
        double amortizedMonthlyPayment = AmortizationCalculator.getAmortizedMonthlyPayment(amount, rate, 2, terms.getAmortizationPeriodMonths());
        Money regularPayment = new Money(BigDecimal.valueOf(amortizedMonthlyPayment), amount.getCurrency(), amount.getRoundingMode());
        terms.setRegularPayment(regularPayment);
        
        LocalDate today = LocalDate.now();
        LocalDate adjDate = LocalDate.now();
        
        int dayOfMonth = today.getDayOfMonth();
        if (1 != dayOfMonth) {
            if (dayOfMonth > 15) {
              adjDate = LocalDate.of(today.getYear(), today.getMonthValue(), 1);
              adjDate = adjDate.plusMonths(1);
            } else if (dayOfMonth < 15) {
              adjDate = LocalDate.of(today.getYear(), today.getMonthValue(), 15);
            }
        }
        
        terms.setStartDate(today);
        terms.setAdjustmentDate(adjDate);
        
        Iterator<ScheduledPayment> result = AmortizationCalculator.getPayments(terms);
        
        int resultCount = 0;
        Money interestTotal = new Money("0.00");
        while(result.hasNext()) {
            resultCount++;
            ScheduledPayment payment = result.next();
            interestTotal = interestTotal.add(payment.getInterest());
            //System.out.println("" + payment);
        }
        assertEquals("Amortized payment count", termInMonths, resultCount);
        assertEquals("Amortized Interest total", new Money("45681.34"), interestTotal);

    }


    /**
     * Sum of principal payments should match original balance minus final balance.
     * 
     */
    @Test
    public void testGetPaymentsAmortizedConsistency() {
        System.out.println("testGetPaymentsAmortizedConsistency");
        
        String originalBalanceString = "20000.00";
        
        Money orignalBalance = new Money(originalBalanceString, Currency.getInstance("CAD"), RoundingMode.HALF_UP);
        double rate = 10.0;
        int termInMonths = 12;
        
        AmortizationAttributes terms = new AmortizationAttributes();
        terms.setInterestOnly(false);
        terms.setLoanAmount(orignalBalance);
        terms.setTermInMonths(termInMonths);
        terms.setAmortizationPeriodMonths(10 * 12); // 10 years
        terms.setCompoundingPeriodsPerYear(2);
        terms.setInterestRate(rate);
        
        double amortizedMonthlyPayment = AmortizationCalculator.getAmortizedMonthlyPayment(orignalBalance, rate, 2, terms.getAmortizationPeriodMonths());
        Money regularPayment = new Money(BigDecimal.valueOf(amortizedMonthlyPayment), orignalBalance.getCurrency(), orignalBalance.getRoundingMode());
        terms.setRegularPayment(regularPayment);
        
        terms.setStartDate(LocalDate.of(2014, 1, 1));
        terms.setAdjustmentDate(LocalDate.of(2014, 1, 1));
        
        
        Iterator<ScheduledPayment> result = AmortizationCalculator.getPayments(terms);
        
        int resultCount = 0;
        Money principalTotal = new Money("0.00");
        
        while(result.hasNext()) {
            
            resultCount++;
            ScheduledPayment payment = result.next();
            principalTotal = principalTotal.add(payment.getPrincipal());
            
            assertEquals("Remaining balance is original balance minus principal paid so far " + resultCount
                    ,orignalBalance.subtract(principalTotal)
                    ,payment.getBalance()
            );
            
            assertEquals("Interest + Principal matches periodic payment"
                    ,payment.getPayment()
                    ,payment.getInterest().add(payment.getPrincipal())
            );

        }

    }

    
    /**
     * Payments should not result in a negative balance.
     * 
     */
    @Test
    public void testGetPaymentsAmortizedNoNegativeBalance() {
        System.out.println("testGetPaymentsAmortizedNoNegativeBalance");
        
        String originalBalanceString = "20000.00";
        
        Money orignalBalance = new Money(originalBalanceString, Currency.getInstance("CAD"), RoundingMode.HALF_UP);
        double rate = 10.0;
        int termInMonths = 12;
        
        AmortizationAttributes terms = new AmortizationAttributes();
        terms.setInterestOnly(false);
        terms.setLoanAmount(orignalBalance);
        terms.setTermInMonths(termInMonths);
        terms.setAmortizationPeriodMonths(10 * 12); // 10 years
        terms.setCompoundingPeriodsPerYear(2);
        terms.setInterestRate(rate);
        terms.setRegularPayment(new Money("5000"));
        terms.setStartDate(LocalDate.of(2014, 1, 1));
        terms.setAdjustmentDate(LocalDate.of(2014, 1, 1));
        
        
        Iterator<ScheduledPayment> result = AmortizationCalculator.getPayments(terms);
        
        int resultCount = 0;
        Money zero = new Money("0.00");
        
        while(result.hasNext()) {
            
            ScheduledPayment payment = result.next();
            
            assertTrue("Balance should be above zero", payment.getBalance().compareTo(zero) >= 0);
            
            assertEquals("Interest + Principal matches periodic payment"
                    ,payment.getPayment()
                    ,payment.getInterest().add(payment.getPrincipal())
            );

        }

    }

    
    /**
     * Test of getInterestOnlyMonthlyPayment method, of class AmortizationCalculator.
     */
    @Test
    public void testGetInterestOnlyMonthlyPayment() {
        System.out.println("getInterestOnlyMonthlyPayment");
        Money amount = new Money("100000.00");
        double rate = 12.0;
        Money expResult = new Money("1000.00");
        
        double interestOnlyMonthlyPayment = AmortizationCalculator.getInterestOnlyMonthlyPayment(amount.getAmount().doubleValue(), rate);
        Money result = new Money(BigDecimal.valueOf(interestOnlyMonthlyPayment));
        
        assertEquals(expResult, result);
    }

    /**
     * Test of getAmortizedMonthlyPayment method, of class AmortizationCalculator.
     */
    @Test
    public void testGetAmortizedMonthlyPayment() {
        System.out.println("getAmortizedMonthlyPayment");
        Money loanAmount = new Money("100000.00", Currency.getInstance("CAD"), RoundingMode.CEILING);
        double i = 12.0;
        int compoundPeriod = 2;
        int amortizationPeriod = 25 * 12;
        Money expResult = new Money("1031.90");
        
        double amortizedMonthlyPayment = AmortizationCalculator.getAmortizedMonthlyPayment(loanAmount, i, compoundPeriod, amortizationPeriod);
        Money result = new Money(BigDecimal.valueOf(amortizedMonthlyPayment), loanAmount.getCurrency(), loanAmount.getRoundingMode());
        
        assertEquals("Semi-annual compounding period", expResult, result);
        
        compoundPeriod = 12;
        loanAmount = new Money("100000.00", Currency.getInstance("CAD"), RoundingMode.HALF_UP);
        expResult = new Money("1053.22");
        
        amortizedMonthlyPayment = AmortizationCalculator.getAmortizedMonthlyPayment(loanAmount, i, compoundPeriod, amortizationPeriod);
        result = new Money(BigDecimal.valueOf(amortizedMonthlyPayment), loanAmount.getCurrency(), loanAmount.getRoundingMode());
        
        assertEquals("Monthly compound period", expResult, result);

        compoundPeriod = 12;
        loanAmount = new Money("100000.00", Currency.getInstance("CAD"), RoundingMode.CEILING);
        expResult = new Money("1053.23");
        
        amortizedMonthlyPayment = AmortizationCalculator.getAmortizedMonthlyPayment(loanAmount, i, compoundPeriod, amortizationPeriod);
        result = new Money(BigDecimal.valueOf(amortizedMonthlyPayment), loanAmount.getCurrency(), loanAmount.getRoundingMode());
        
        assertEquals("Monthly compound period, ceiling", expResult, result);

    }

    
    @Test
    public void testPerDiem() {
        System.out.println("perDiem");
        Money loanAmount = new Money("100000.00", Currency.getInstance("CAD"), RoundingMode.CEILING);
        double i = 12.0;
        Money expResult = new Money("32.88");
       
        Money perDiemResult = AmortizationCalculator.getPerDiem(loanAmount, i);
        
        assertEquals("Per Diem", expResult, perDiemResult);
    }
    
    @Test
    public void testAdjustmentAmount() {
        System.out.println("adjustment");
        Money loanAmount = new Money("100000.00", Currency.getInstance("CAD"), RoundingMode.CEILING);
        double i = 12.0;
        int days = 7;
        Money expResult = new Money("32.88").multiply(days);
       
        Money adjustmentResult = AmortizationCalculator.getAdjustmentAmount(loanAmount, i, days);
        
        assertEquals("Per Diem", expResult, adjustmentResult);
    }
    
}
